package com.pratham.livo.service.impl;

import com.pratham.livo.config.RabbitMQConfig;
import com.pratham.livo.dto.message.EmailMessage;
import com.pratham.livo.dto.message.PaymentMessage;
import com.pratham.livo.dto.message.RefundMessage;
import com.pratham.livo.dto.message.RefundUpdateMessage;
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

    @Override
    public void publishRefund(RefundMessage refundMessage) {
        log.info("Queuing refund for razorpayPaymentId: {}",refundMessage.getRazorpayPaymentId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.MAIN_EXCHANGE,
                RabbitMQConfig.REFUND_ROUTING_KEY,
                refundMessage
        );
    }

    @Override
    public void publishRefundUpdate(RefundUpdateMessage refundUpdateMessage) {
        log.info("Queuing refund update for razorpayRefundId: {}",refundUpdateMessage.getRazorpayRefundId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.MAIN_EXCHANGE,
                RabbitMQConfig.REFUND_UPDATE_ROUTING_KEY,
                refundUpdateMessage
        );
    }


}
