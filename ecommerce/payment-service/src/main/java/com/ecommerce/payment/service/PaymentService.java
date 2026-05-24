package com.ecommerce.payment.service;

import com.ecommerce.payment.config.RabbitMQConfig;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.events.PaymentEvents;
import com.ecommerce.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate   rabbitTemplate;

    /**
     * Tasa de fallo simulada — configurable en application.properties.
     * app.payment.failure-rate=0.2 → 20% de los pagos fallan.
     * Permite demostrar la compensación Saga sin integrar una pasarela real.
     */
    @Value("${app.payment.failure-rate:0.2}")
    private double failureRate;

    /**
     * Procesa el pago de una orden recibida via evento OrderCreated.
     *
     * Flujo:
     * 1. Registrar el pago en estado PENDING en payments_db
     * 2. Simular procesamiento (configurable para forzar éxito o fallo)
     * 3. Actualizar estado en BD
     * 4. Publicar PaymentSucceededEvent o PaymentFailedEvent
     *
     * La transacción garantiza que la escritura en BD y la publicación
     * del evento son coherentes. (Para garantía total usar patrón Outbox)
     */
    @Transactional
    public void processPayment(PaymentEvents.OrderCreatedEvent event) {

        // Idempotencia: si ya existe un pago para esta orden, no procesar de nuevo
        if (paymentRepository.findByOrderId(event.orderId()).isPresent()) {
            log.warn("[PAYMENT] Pago duplicado ignorado para orderId={}", event.orderId());
            return;
        }

        log.info("[PAYMENT] Procesando pago → orderId={}, total={}", event.orderId(), event.total());

        // 1. Registrar en BD como PENDING
        Payment payment = Payment.builder()
                .orderId(event.orderId())
                .userId(event.userId())
                .amount(event.total())
                .status(Payment.Status.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        // 2. Simular procesamiento del pago
        boolean approved = simulatePaymentGateway();

        // 3. Actualizar estado
        payment.setProcessedAt(LocalDateTime.now());

        if (approved) {
            String txId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            payment.setStatus(Payment.Status.SUCCESS);
            payment.setTransactionId(txId);
            paymentRepository.save(payment);

            // 4a. Publicar evento de éxito → monolito confirma la orden
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_SUCCEEDED_KEY,
                new PaymentEvents.PaymentSucceededEvent(
                    event.orderId(), payment.getId(), txId, LocalDateTime.now())
            );

            log.info("[PAYMENT] ✅ Pago aprobado → orderId={}, txId={}", event.orderId(), txId);

        } else {
            String reason = "Pago rechazado por la pasarela (simulado)";
            payment.setStatus(Payment.Status.FAILED);
            payment.setFailureReason(reason);
            paymentRepository.save(payment);

            // 4b. Publicar evento de fallo → monolito compensa (libera stock + cancela orden)
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_FAILED_KEY,
                new PaymentEvents.PaymentFailedEvent(
                    event.orderId(), reason, LocalDateTime.now())
            );

            log.warn("[PAYMENT] ❌ Pago rechazado → orderId={}, razón={}", event.orderId(), reason);
        }
    }

    /**
     * Simula una pasarela de pago externa.
     * En producción, aquí iría la llamada HTTP a Stripe, PayU, etc.
     * La tasa de fallo es configurable para demos de compensación Saga.
     */
    private boolean simulatePaymentGateway() {
        try {
            // Simula latencia de red con la pasarela (200-800ms)
            Thread.sleep((long) (200 + Math.random() * 600));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Math.random() >= failureRate;
    }

    // ── Consultas ────────────────────────────────────────────

    public Optional<Payment> findByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }

    public List<Payment> findByStatus(Payment.Status status) {
        return paymentRepository.findByStatus(status);
    }
}
