package com.pratham.livo.controller;

import com.pratham.livo.dto.payment.PaymentInitResponseDto;
import com.pratham.livo.dto.payment.PaymentVerifyRequestDto;
import com.pratham.livo.exception.BadRequestException;
import com.pratham.livo.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{bookingId}/init")
    public ResponseEntity<PaymentInitResponseDto> initPayment(
            @PathVariable Long bookingId,
            @RequestHeader(name = "Idempotency-Key") UUID idempotencyKey){
        log.info("Attempting to initiate payment for booking with id: {}",bookingId);
        return ResponseEntity.ok(paymentService.initPayment(bookingId,idempotencyKey));
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verifyPayment(
            @RequestBody PaymentVerifyRequestDto paymentVerifyRequestDto){
        log.info("Attempting to verify payment with razorpayOrderId: {}",paymentVerifyRequestDto.getRazorpayOrderId());
        boolean isLegit = paymentService.verifyPaymentFromClient(paymentVerifyRequestDto);
        if(!isLegit){
            throw new BadRequestException("Payment Verification Failed");
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(name = "X-Razorpay-Signature") String webhookSignature
    ){
        paymentService.processWebhook(payload,webhookSignature);
        return ResponseEntity.noContent().build();
    }
}
