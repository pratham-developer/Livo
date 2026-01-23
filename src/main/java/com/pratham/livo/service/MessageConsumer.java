package com.pratham.livo.service;

import com.pratham.livo.dto.message.EmailMessage;
import com.pratham.livo.dto.message.PaymentMessage;

public interface MessageConsumer {
    void consumeEmail(EmailMessage emailMessage);
    void consumePaymentForWebhook(PaymentMessage paymentMessage);
}
