package com.ecommerce.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Los nombres de exchanges y colas DEBEN coincidir exactamente con los del monolito.
 * Ambos servicios declaran los mismos recursos: RabbitMQ es idempotente en la
 * declaración — si ya existen con las mismas propiedades, no hace nada.
 */
@Configuration
public class RabbitMQConfig {

    // Nombres compartidos con el monolito
    public static final String ECOMMERCE_EXCHANGE = "ecommerce.events";
    public static final String PAYMENT_EXCHANGE   = "payment.events";
    public static final String DLX_EXCHANGE       = "ecommerce.dlx";

    public static final String ORDER_CREATED_KEY     = "order.created";
    public static final String PAYMENT_SUCCEEDED_KEY = "payment.succeeded";
    public static final String PAYMENT_FAILED_KEY    = "payment.failed";

    public static final String ORDER_CREATED_QUEUE  = "order.created.queue";
    public static final String PAYMENT_RESULT_QUEUE = "payment.result.queue";
    public static final String ORDER_CREATED_DLQ    = "order.created.dlq";

    @Bean public TopicExchange ecommerceExchange() {
        return ExchangeBuilder.topicExchange(ECOMMERCE_EXCHANGE).durable(true).build(); }

    @Bean public TopicExchange paymentExchange() {
        return ExchangeBuilder.topicExchange(PAYMENT_EXCHANGE).durable(true).build(); }

    @Bean public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DLX_EXCHANGE).durable(true).build(); }

    @Bean public Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_CREATED_DLQ)
                .build(); }

    @Bean public Queue paymentResultQueue() {
        return QueueBuilder.durable(PAYMENT_RESULT_QUEUE).build(); }

    @Bean public Queue orderCreatedDlq() {
        return QueueBuilder.durable(ORDER_CREATED_DLQ).build(); }

    @Bean public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue()).to(ecommerceExchange()).with(ORDER_CREATED_KEY); }

    @Bean public Binding paymentSucceededBinding() {
        return BindingBuilder.bind(paymentResultQueue()).to(paymentExchange()).with(PAYMENT_SUCCEEDED_KEY); }

    @Bean public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentResultQueue()).to(paymentExchange()).with(PAYMENT_FAILED_KEY); }

    @Bean public Binding dlqBinding() {
        return BindingBuilder.bind(orderCreatedDlq()).to(deadLetterExchange()).with(ORDER_CREATED_DLQ); }

    @Bean public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter(); }

    @Bean public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(jsonMessageConverter());
        return t; }
}
