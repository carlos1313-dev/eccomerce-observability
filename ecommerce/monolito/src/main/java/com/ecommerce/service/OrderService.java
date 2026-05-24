package com.ecommerce.service;

import com.ecommerce.audit.AuditService;
import com.ecommerce.dto.Dtos;
import com.ecommerce.entity.*;
import com.ecommerce.events.EventPublisher;
import com.ecommerce.events.Events;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository    orderRepository;
    private final ProductRepository  productRepository;
    private final UserRepository     userRepository;
    private final AuditService       auditService;
    private final EventPublisher     eventPublisher;   // ← NUEVO: publicador de eventos

    /**
     * FLUJO SAGA — Paso 1: crear la orden y publicar OrderCreated.
     *
     * Cambios respecto al Taller 1:
     *  - La orden empieza en estado PENDING (no CONFIRMED).
     *  - Al final de la transacción se publica el evento OrderCreated.
     *  - El estado final (CONFIRMED / CANCELLED) lo determina el listener
     *    de compensación al recibir la respuesta del payment-service.
     *
     * El stock se descuenta de inmediato (bloqueo pesimista) para evitar
     * sobreventa. Si el pago falla, el listener de compensación lo restaura.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Dtos.OrderResponse createOrder(Dtos.CreateOrderRequest request, String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userEmail));

        // Ordenamos IDs para evitar deadlocks entre transacciones concurrentes
        List<Long> sortedProductIds = request.items().stream()
                .map(Dtos.OrderItemRequest::productId)
                .distinct().sorted().toList();

        List<Product> lockedProducts = sortedProductIds.stream()
                .map(id -> productRepository.findByIdWithPessimisticLock(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id)))
                .toList();

        List<OrderItem> items = request.items().stream().map(itemReq -> {
            Product product = lockedProducts.stream()
                    .filter(p -> p.getId().equals(itemReq.productId()))
                    .findFirst().orElseThrow();

            try {
                product.decreaseStock(itemReq.quantity());
            } catch (IllegalStateException e) {
                throw new InsufficientStockException(e.getMessage());
            }

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));
            return OrderItem.builder()
                    .product(product)
                    .quantity(itemReq.quantity())
                    .unitPrice(product.getPrice())
                    .subtotal(subtotal)
                    .build();
        }).toList();

        BigDecimal total = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .user(user)
                .total(total)
                .status(Order.Status.PENDING)   // ← SAGA: empieza en PENDING
                .build();

        items.forEach(item -> {
            item.setOrder(order);
            order.getItems().add(item);
        });

        Order saved = orderRepository.save(order);

        auditService.logSuccess("CREATE_ORDER", "ORDER", saved.getId(), userEmail,
                "Orden PENDING creada con %d ítem(s), total: %s".formatted(items.size(), total));

        // ── SAGA Paso 1: publicar evento al payment-service ───────────────
        List<Events.OrderCreatedEvent.OrderItem> eventItems = items.stream()
                .map(i -> new Events.OrderCreatedEvent.OrderItem(
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getQuantity(),
                        i.getUnitPrice()))
                .toList();

        eventPublisher.publishOrderCreated(new Events.OrderCreatedEvent(
                saved.getId(), user.getId(), userEmail,
                eventItems, total, LocalDateTime.now()
        ));
        // ─────────────────────────────────────────────────────────────────

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<Dtos.OrderResponse> getMyOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return orderRepository.findByUserIdWithItems(user.getId()).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<Dtos.OrderResponse> getAllOrders() {
        return orderRepository.findAllWithItems().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Dtos.OrderResponse getOrderById(Long id, String userEmail, boolean isAdmin) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + id));
        if (!isAdmin && !order.getUser().getEmail().equals(userEmail)) {
            auditService.logDenied("VIEW_ORDER", "ORDER", id, userEmail,
                    "Intento de acceso a orden de otro usuario");
            throw new AccessDeniedException("No tiene permiso para ver esta orden");
        }
        return toResponse(order);
    }

    private Dtos.OrderResponse toResponse(Order order) {
        List<Dtos.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new Dtos.OrderItemResponse(
                        item.getProduct().getId(), item.getProduct().getName(),
                        item.getQuantity(), item.getUnitPrice(), item.getSubtotal()))
                .toList();

        return new Dtos.OrderResponse(
                order.getId(), order.getUser().getId(), order.getUser().getName(),
                itemResponses, order.getTotal(), order.getStatus().name(), order.getCreatedAt()
        );
    }
}
