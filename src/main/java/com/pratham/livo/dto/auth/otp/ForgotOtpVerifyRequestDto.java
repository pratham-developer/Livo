package com.pratham.livo.dto.auth.otp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ForgotOtpVerifyRequestDto {
    private String registrationId;
    private String otp;
    private String newPassword;
}
