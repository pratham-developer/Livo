package com.pratham.livo.dto.auth.otp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignupOtpVerifyRequestDto {
    private String registrationId;
    private String otp;
}
