package com.pratham.livo.dto.auth.otp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class OtpHelperDto {
    private String registrationId;
    private String otp;
    private long nextResendAt;
    private String email;
}
