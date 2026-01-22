package com.pratham.livo.service.impl;

import brevo.ApiException;
import brevoApi.TransactionalEmailsApi;
import brevoModel.SendSmtpEmail;
import brevoModel.SendSmtpEmailSender;
import brevoModel.SendSmtpEmailTo;
import com.pratham.livo.config.RabbitMQConfig;
import com.pratham.livo.dto.message.EmailMessage;
import com.pratham.livo.service.EmailConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailConsumerImpl implements EmailConsumer {

    private final TransactionalEmailsApi transactionalEmailsApi;

    @Value("${livo.email.from.email}")
    private String fromEmail;

    @Value("${livo.email.from.name}")
    private String fromName;

    @Override
    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void consumeEmail(EmailMessage emailMessage) {
        try{
            log.info("Sending email to {}", emailMessage.getTo());
            sendWithBrevo(emailMessage);
        }catch (ApiException e){
            throw new RuntimeException("Retryable Brevo API Error", e);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendWithBrevo(EmailMessage emailMessage) throws ApiException {
        SendSmtpEmail email = new SendSmtpEmail();

        email.setSender(new SendSmtpEmailSender().email(fromEmail).name(fromName));
        email.setTo(Collections.singletonList(new SendSmtpEmailTo().email(emailMessage.getTo())));

        email.setSubject(emailMessage.getSubject());
        email.setHtmlContent(emailMessage.getHtmlContent());

        email.setTags(Arrays.asList("authentication", "security", "otp"));

        transactionalEmailsApi.sendTransacEmail(email);
    }
}
