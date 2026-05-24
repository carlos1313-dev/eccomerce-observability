package com.ecommerce.payment.events;

import com.ecommerce.payment.config.RabbitMQConfig;
import com.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener que consume eventos OrderCreated publicados por el monolito.
 *
 * Cada mensaje que llega desencadena el proceso de pago completo.
 *
 * Si el procesamiento lanza una excepción no controlada, RabbitMQ
 * reintenta hasta max-attempts (configurado en application.properties).
 * Tras agotar los reintentos, el mensaje pasa a la DLQ (order.created.dlq).
 *
 * RESPUESTA A LA PREGUNTA DE REVISIÓN:
 * "¿Qué pasa si el payment-service está caído?"
 * → RabbitMQ conserva los mensajes en order.created.queue (durable=true).
 * → Cuando el servicio vuelve, los procesa en orden.
 * → Si agota reintentos antes de volver, el mensaje va a la DLQ
 *   para análisis manual o re-encolado por el operador.
 * → El cliente recibe 202 ACCEPTED inmediatamente; el estado final
 *   lo consulta haciendo polling a GET /api/orders/{id}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedListener {

    private final PaymentService paymentService;
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public OrderCreatedListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void onOrderCreated(PaymentEvents.OrderCreatedEvent event) {
        log.info("[LISTENER] OrderCreated recibido → orderId={}, total={}",
                event.orderId(), event.total());
        try {
            paymentService.processPayment(event);
        } catch (Exception e) {
            log.error("[LISTENER] Error procesando pago para orderId={}: {}",
                    event.orderId(), e.getMessage(), e);
            // Re-lanzar para que RabbitMQ aplique la política de reintento/DLQ
            throw new RuntimeException("Error en procesamiento de pago", e);
        }
    }
}
