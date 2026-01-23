package com.pratham.livo.service.impl;

import com.pratham.livo.config.RabbitMQConfig;
import com.pratham.livo.dto.message.EmailMessage;
import com.pratham.livo.dto.message.PaymentMessage;
import com.pratham.livo.service.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessagePublisherImpl implements MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishEmail(EmailMessage emailMessage) {
        log.info("Queuing email for {}",emailMessage.getTo());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.MAIN_EXCHANGE,
                RabbitMQConfig.EMAIL_ROUTING_KEY,
                emailMessage
        );
    }

    @Override
    public void publishPaymentForWebhook(PaymentMessage paymentMessage) {
        log.info("Queuing payment for razorpayOrderId: {}",paymentMessage.getRazorpayOrderId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.MAIN_EXCHANGE,
                RabbitMQConfig.PAYMENT_ROUTING_KEY,
                paymentMessage
        );
    }


}
