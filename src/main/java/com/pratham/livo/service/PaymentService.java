package com.pratham.livo.service;


import com.pratham.livo.dto.payment.PaymentInitResponseDto;
import com.pratham.livo.dto.payment.PaymentVerifyRequestDto;

import java.util.UUID;

public interface PaymentService {
    PaymentInitResponseDto initPayment(Long bookingId, UUID idempotencyKey);
    boolean verifyPaymentFromClient(PaymentVerifyRequestDto paymentVerifyRequestDto);
    void processWebhook(String payload, String webhookSignature);
}
