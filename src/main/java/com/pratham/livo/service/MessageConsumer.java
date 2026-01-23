package com.pratham.livo.service;

import com.pratham.livo.dto.message.EmailMessage;
import com.pratham.livo.dto.message.PaymentMessage;
import com.pratham.livo.dto.message.RefundMessage;
import com.pratham.livo.dto.message.RefundUpdateMessage;

public interface MessageConsumer {
    void consumeEmail(EmailMessage emailMessage);
    void consumePaymentForWebhook(PaymentMessage paymentMessage);
    void consumeRefund(RefundMessage refundMessage);
    void consumeRefundUpdate(RefundUpdateMessage refundUpdateMessage);
}

