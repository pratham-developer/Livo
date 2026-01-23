package com.pratham.livo.service;

import com.pratham.livo.dto.message.EmailMessage;
import com.pratham.livo.dto.message.PaymentMessage;
import com.pratham.livo.dto.message.RefundMessage;
import com.pratham.livo.dto.message.RefundUpdateMessage;

public interface MessagePublisher {
    void publishEmail(EmailMessage emailMessage);
    void publishPaymentForWebhook(PaymentMessage paymentMessage);
    void publishRefund(RefundMessage refundMessage);
    void publishRefundUpdate(RefundUpdateMessage refundUpdateMessage);
}
