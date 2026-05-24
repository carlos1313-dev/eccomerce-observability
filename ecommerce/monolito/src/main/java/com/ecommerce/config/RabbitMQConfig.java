package com.ecommerce.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CONFIGURACIÓN RABBITMQ — Exchanges, Queues y Bindings
 *
 * Arquitectura de mensajería:
 *
 *  [Monolito] ──► exchange: ecommerce.events (topic) ──► queue: order.created.queue
 *                                                              └──► [Payment Service consume]
 *
 *  [Payment Service] ──► exchange: payment.events (topic) ──► queue: payment.result.queue
 *                                                                  └──► [Monolito consume]
 *
 * Dead Letter Queue (DLQ):
 *  Si un mensaje falla 3 veces, va a: order.created.dlq
 *  Esto responde la pregunta de revisión: "¿qué pasa si el payment-service está caído?"
 *  → Los mensajes se acumulan en la DLQ, no se pierden. Cuando el servicio
 *    vuelve, el operador puede re-encolar o analizar los mensajes fallidos.
 *
 * durable=true: las colas y exchanges sobreviven reinicios de RabbitMQ.
 */
@Configuration
public class RabbitMQConfig {

    // ── Exchanges ─────────────────────────────────────────────
    public static final String ECOMMERCE_EXCHANGE  = "ecommerce.events";
    public static final String PAYMENT_EXCHANGE    = "payment.events";
    public static final String DLX_EXCHANGE        = "ecommerce.dlx";

    // ── Routing keys ──────────────────────────────────────────
    public static final String ORDER_CREATED_KEY       = "order.created";
    public static final String PAYMENT_SUCCEEDED_KEY   = "payment.succeeded";
    public static final String PAYMENT_FAILED_KEY      = "payment.failed";

    // ── Queue names ───────────────────────────────────────────
    public static final String ORDER_CREATED_QUEUE     = "order.created.queue";
    public static final String PAYMENT_RESULT_QUEUE    = "payment.result.queue";
    public static final String ORDER_CREATED_DLQ       = "order.created.dlq";

    // ── Exchanges ─────────────────────────────────────────────

    @Bean
    public TopicExchange ecommerceExchange() {
        return ExchangeBuilder.topicExchange(ECOMMERCE_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange paymentExchange() {
        return ExchangeBuilder.topicExchange(PAYMENT_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DLX_EXCHANGE).durable(true).build();
    }

    // ── Queues ────────────────────────────────────────────────

    /**
     * Cola que consume el payment-service.
     * Configurada con DLX: si el mensaje falla 3 veces, va a order.created.dlq.
     */
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_CREATED_DLQ)
                .build();
    }

    /**
     * Cola que consume el monolito con los resultados del pago.
     */
    @Bean
    public Queue paymentResultQueue() {
        return QueueBuilder.durable(PAYMENT_RESULT_QUEUE).build();
    }

    /**
     * Dead Letter Queue — mensajes que no pudieron ser procesados.
     * Se monitorea en el dashboard de RabbitMQ Management (puerto 15672).
     */
    @Bean
    public Queue orderCreatedDlq() {
        return QueueBuilder.durable(ORDER_CREATED_DLQ).build();
    }

    // ── Bindings ──────────────────────────────────────────────

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder
                .bind(orderCreatedQueue())
                .to(ecommerceExchange())
                .with(ORDER_CREATED_KEY);
    }

    @Bean
    public Binding paymentSucceededBinding() {
        return BindingBuilder
                .bind(paymentResultQueue())
                .to(paymentExchange())
                .with(PAYMENT_SUCCEEDED_KEY);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder
                .bind(paymentResultQueue())
                .to(paymentExchange())
                .with(PAYMENT_FAILED_KEY);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder
                .bind(orderCreatedDlq())
                .to(deadLetterExchange())
                .with(ORDER_CREATED_DLQ);
    }

    // ── Serialización JSON ────────────────────────────────────

    /**
     * Convierte los eventos (records) a JSON automáticamente.
     * Sin esto, RabbitMQ serializa los objetos en binario Java (no portable).
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
