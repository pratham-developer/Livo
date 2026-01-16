package com.pratham.livo.dto.auth.otp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InitiateOtpResponseDto {
    private String registrationId;
    private long nextResendAt;
}
