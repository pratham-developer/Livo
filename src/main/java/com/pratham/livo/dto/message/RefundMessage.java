package com.pratham.livo.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefundMessage {
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String reason;
    private int percentage;
}
