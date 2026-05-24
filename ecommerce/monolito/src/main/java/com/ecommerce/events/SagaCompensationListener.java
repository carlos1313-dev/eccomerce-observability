package com.ecommerce.events;

import com.ecommerce.audit.AuditService;
import com.ecommerce.config.RabbitMQConfig;
import com.ecommerce.entity.AuditLog;
import com.ecommerce.entity.Order;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * LISTENER DE COMPENSACIÓN SAGA
 *
 * Escucha los resultados del payment-service y actúa:
 *
 *  PaymentSucceeded → orden pasa a CONFIRMED
 *  PaymentFailed    → COMPENSACIÓN: libera stock + cancela orden
 *
 * La compensación es la parte más crítica de la Saga:
 * garantiza que si el pago falla, el inventario reservado
 * se devuelve y la orden queda en estado consistente.
 *
 * @Transactional garantiza que ambas operaciones (stock + orden)
 * ocurren atómicamente: o las dos se aplican, o ninguna.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaCompensationListener {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AuditService auditService;

    /**
     * HAPPY PATH: pago aprobado → confirmar orden.
     */
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_RESULT_QUEUE,
                    messageConverter = "jsonMessageConverter")
    @Transactional
    public void handlePaymentResult(Object rawEvent) {
        if (rawEvent instanceof Events.PaymentSucceededEvent event) {
            handlePaymentSucceeded(event);
        } else if (rawEvent instanceof Events.PaymentFailedEvent event) {
            handlePaymentFailed(event);
        } else {
            log.warn("[SAGA] Evento desconocido recibido: {}", rawEvent.getClass().getSimpleName());
        }
    }

    private void handlePaymentSucceeded(Events.PaymentSucceededEvent event) {
        log.info("[SAGA] PaymentSucceeded recibido → orderId={}, txId={}",
                event.orderId(), event.transactionId());

        orderRepository.findById(event.orderId()).ifPresentOrElse(order -> {
            order.setStatus(Order.Status.CONFIRMED);
            orderRepository.save(order);

            auditService.logSuccess(
                "PAYMENT_SUCCEEDED", "ORDER", event.orderId(),
                "system",
                "Orden confirmada. TransactionId: " + event.transactionId()
            );
            log.info("[SAGA] Orden {} confirmada exitosamente", event.orderId());

        }, () -> log.error("[SAGA] Orden no encontrada para confirmar: {}", event.orderId()));
    }

    /**
     * COMPENSACIÓN: pago rechazado.
     *
     * Pasos:
     * 1. Recuperar la orden y sus ítems
     * 2. Devolver el stock a cada producto (liberación)
     * 3. Cambiar el estado de la orden a CANCELLED
     *
     * Si cualquier paso falla, @Transactional hace rollback completo
     * y RabbitMQ reintentará según la política de retry configurada.
     */
    private void handlePaymentFailed(Events.PaymentFailedEvent event) {
        log.warn("[SAGA] PaymentFailed recibido → orderId={}, razón={}",
                event.orderId(), event.reason());

        orderRepository.findByIdWithItems(event.orderId()).ifPresentOrElse(order -> {

            // 1. Liberar stock de cada ítem de la orden
            order.getItems().forEach(item -> {
                productRepository.findById(item.getProduct().getId()).ifPresent(product -> {
                    product.increaseStock(item.getQuantity());
                    productRepository.save(product);
                    log.info("[SAGA] Stock liberado → producto={}, cantidad={}",
                            product.getName(), item.getQuantity());
                });
            });

            // 2. Cancelar la orden
            order.setStatus(Order.Status.CANCELLED);
            orderRepository.save(order);

            auditService.logFailure(
                "PAYMENT_FAILED_COMPENSATION", "ORDER", event.orderId(),
                "system",
                "Orden cancelada y stock liberado. Razón: " + event.reason()
            );
            log.warn("[SAGA] Compensación completada → orden {} cancelada, stock restaurado",
                    event.orderId());

        }, () -> log.error("[SAGA] Orden no encontrada para compensar: {}", event.orderId()));
    }
}
