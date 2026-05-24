package com.ecommerce.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Eventos que viajan por RabbitMQ entre el monolito y el payment-service.
 *
 * Se usan records porque los eventos son inmutables: representan un hecho
 * que ya ocurrió y no deben modificarse en tránsito.
 *
 * Se implementa Serializable para que Jackson pueda serializarlos a JSON.
 */
public class Events {

    // ============================================================
    // EVENTOS QUE PUBLICA EL MONOLITO
    // ============================================================

    /**
     * Publicado cuando una orden es creada y el stock reservado.
     * El payment-service lo consume para iniciar el proceso de pago.
     */
    public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        String userEmail,
        List<OrderItem> items,
        BigDecimal total,
        LocalDateTime occurredAt
    ) {
        public record OrderItem(Long productId, String productName, Integer quantity, BigDecimal unitPrice) {}
    }

    // ============================================================
    // EVENTOS QUE CONSUME EL MONOLITO (publicados por payment-service)
    // ============================================================

    /**
     * Publicado por payment-service cuando el pago es aprobado.
     * El monolito actualiza la orden a CONFIRMED.
     */
    public record PaymentSucceededEvent(
        Long orderId,
        Long paymentId,
        String transactionId,
        LocalDateTime occurredAt
    ) {}

    /**
     * EVENTO DE COMPENSACIÓN — publicado por payment-service cuando el pago falla.
     * El monolito debe:
     *   1. Liberar el stock reservado (StockReleased)
     *   2. Cancelar la orden (OrderCancelled)
     * Estos dos pasos son la "transacción de compensación" de la Saga.
     */
    public record PaymentFailedEvent(
        Long orderId,
        String reason,
        LocalDateTime occurredAt
    ) {}
}
