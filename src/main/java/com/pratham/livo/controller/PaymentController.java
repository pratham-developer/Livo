package com.pratham.livo.controller;

import com.pratham.livo.dto.payment.PaymentInitResponseDto;
import com.pratham.livo.dto.payment.PaymentVerifyRequestDto;
import com.pratham.livo.exception.BadRequestException;
import com.pratham.livo.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{bookingId}/init")
    public ResponseEntity<PaymentInitResponseDto> initPayment(
            @PathVariable Long bookingId,
            @NotNull(message = "Idempotency-Key header is required")
            @RequestHeader(name = "Idempotency-Key") UUID idempotencyKey){
        log.info("Attempting to initiate payment for booking with id: {}",bookingId);
        return ResponseEntity.ok(paymentService.initPayment(bookingId,idempotencyKey));
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verifyPayment(
            @Valid @RequestBody PaymentVerifyRequestDto paymentVerifyRequestDto){
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
            @RequestHeader(name = "x-razorpay-signature") String webhookSignature,
            @RequestHeader(name = "x-razorpay-event-id") String eventId

    ){
        paymentService.processWebhook(payload,webhookSignature,eventId);
        return ResponseEntity.noContent().build();
    }
}