package com.pratham.livo.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentInitResponseDto {
    private Long bookingId;
    private BigDecimal amount;
    private String currency;
    private String razorpayOrderId;
    private String razorpayKeyId;
    private String companyName;
    private String description;
    private String userEmail;
}
