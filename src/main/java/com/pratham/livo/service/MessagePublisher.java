package com.pratham.livo.service;

import com.pratham.livo.dto.message.EmailMessage;
import com.pratham.livo.dto.message.PaymentMessage;

public interface MessagePublisher {
    void publishEmail(EmailMessage emailMessage);
    void publishPaymentForWebhook(PaymentMessage paymentMessage);
}
