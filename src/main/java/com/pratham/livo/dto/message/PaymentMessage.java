package com.pratham.livo.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentMessage {
    private String razorpayOrderId;
    private String razorpayPaymentId;
}
