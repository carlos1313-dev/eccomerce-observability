package com.ecommerce.payment.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Copia local de los eventos compartidos.
 *
 * En un proyecto real se extraería a una librería compartida (shared-events-lib).
 * Para este taller se duplica intencionalmente para mantener los microservicios
 * autónomos y evitar dependencias en tiempo de compilación entre ellos.
 * La comunicación real ocurre por JSON en RabbitMQ, no por clases compartidas.
 */
public class PaymentEvents {

    /** Evento que consume el payment-service — publicado por el monolito */
    public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        String userEmail,
        List<OrderItem> items,
        BigDecimal total,
        LocalDateTime occurredAt
    ) {
        public record OrderItem(Long productId, String productName,
                                Integer quantity, BigDecimal unitPrice) {}
    }

    /** Publicado cuando el pago es aprobado */
    public record PaymentSucceededEvent(
        Long orderId,
        Long paymentId,
        String transactionId,
        LocalDateTime occurredAt
    ) {}

    /** Publicado cuando el pago es rechazado — desencadena compensación Saga */
    public record PaymentFailedEvent(
        Long orderId,
        String reason,
        LocalDateTime occurredAt
    ) {}
}
