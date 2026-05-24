package com.ecommerce.events;

import com.ecommerce.audit.AuditService;
import com.ecommerce.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica eventos de dominio al broker RabbitMQ.
 *
 * Se inyecta en OrderService para publicar OrderCreated
 * inmediatamente después de guardar la orden en base de datos.
 *
 * IMPORTANTE: la publicación ocurre DENTRO de la misma transacción @Transactional.
 * Si RabbitMQ no está disponible, el mensaje falla silenciosamente (logged).
 * Para garantía total de entrega se usaría el patrón Outbox (entregable opcional).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrderCreated(Events.OrderCreatedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ECOMMERCE_EXCHANGE,
                    RabbitMQConfig.ORDER_CREATED_KEY,
                    event
            );
            log.info("[SAGA] OrderCreated publicado → orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("[SAGA] Error publicando OrderCreated para orderId={}: {}",
                    event.orderId(), e.getMessage());
        }
    }
}
