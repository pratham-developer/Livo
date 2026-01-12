package com.pratham.livo.service.impl;

import com.pratham.livo.config.RabbitMQConfig;
import com.pratham.livo.dto.message.EmailMessage;
import com.pratham.livo.service.EmailPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailPublisherImpl implements EmailPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(EmailMessage emailMessage) {
        log.info("Queuing email for {}",emailMessage.getTo());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.MAIN_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                emailMessage
        );
    }
}
