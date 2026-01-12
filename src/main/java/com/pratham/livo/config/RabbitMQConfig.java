package com.pratham.livo.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String MAIN_QUEUE = "email.queue.main";
    public static final String DLQ_QUEUE = "email.queue.dlq";
    public static final String MAIN_EXCHANGE = "email.exchange";
    public static final String DLQ_EXCHANGE = "email.exchange.dlq";
    public static final String ROUTING_KEY = "email.key";
    public static final String DLQ_ROUTING_KEY = "email.dlq.key";

    @Bean
    public Queue mainQueue() {
        return QueueBuilder.durable(MAIN_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue dlqQueue(){
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public TopicExchange mainExchange(){
        return new TopicExchange(MAIN_EXCHANGE);
    }

    @Bean
    public DirectExchange dlqExchange(){
        return new DirectExchange(DLQ_EXCHANGE);
    }

    @Bean
    public Binding mainBinding(){
        return BindingBuilder.bind(mainQueue()).to(mainExchange()).with(ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding(){
        return BindingBuilder.bind(dlqQueue()).to(dlqExchange()).with(DLQ_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter converter(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory){
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter());
        return template;
    }

}
