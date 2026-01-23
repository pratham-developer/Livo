package com.pratham.livo.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EMAIL_QUEUE = "email.msg.queue";
    public static final String PAYMENT_QUEUE = "payment.msg.queue";
    public static final String REFUND_QUEUE = "refund.msq.queue";
    public static final String REFUND_UPDATE_QUEUE = "refund.update.msg.queue";
    public static final String DLQ_QUEUE = "msg.dlq";

    public static final String MAIN_EXCHANGE = "msg.exchange";
    public static final String DLQ_EXCHANGE = "msg.dlq.exchange";

    public static final String EMAIL_ROUTING_KEY = "email.key";
    public static final String PAYMENT_ROUTING_KEY = "payment.key";
    public static final String REFUND_ROUTING_KEY = "refund.key";
    public static final String REFUND_UPDATE_ROUTING_KEY = "refund.update.key";
    public static final String DLQ_ROUTING_KEY = "dlq.key";

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue paymentQueue() {
        return QueueBuilder.durable(PAYMENT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue refundQueue() {
        return QueueBuilder.durable(REFUND_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue refundUpdateQueue() {
        return QueueBuilder.durable(REFUND_UPDATE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(DLQ_QUEUE)
                .withArgument("x-message-ttl", 86400000) // 24 hours
                .build();
    }

    @Bean
    public TopicExchange mainExchange() {
        return new TopicExchange(MAIN_EXCHANGE);
    }

    @Bean
    public DirectExchange dlqExchange() {
        return new DirectExchange(DLQ_EXCHANGE);
    }

    @Bean
    public Binding emailBinding() {
        return BindingBuilder
                .bind(emailQueue())
                .to(mainExchange())
                .with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding paymentBinding() {
        return BindingBuilder
                .bind(paymentQueue())
                .to(mainExchange())
                .with(PAYMENT_ROUTING_KEY);
    }

    @Bean
    public Binding refundBinding() {
        return BindingBuilder
                .bind(refundQueue())
                .to(mainExchange())
                .with(REFUND_ROUTING_KEY);
    }

    @Bean
    public Binding refundUpdateBinding() {
        return BindingBuilder
                .bind(refundUpdateQueue())
                .to(mainExchange())
                .with(REFUND_UPDATE_ROUTING_KEY);
    }


    @Bean
    public Binding dlqBinding() {
        return BindingBuilder
                .bind(dlqQueue())
                .to(dlqExchange())
                .with(DLQ_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonMessageConverter());
        return template;
    }
}
