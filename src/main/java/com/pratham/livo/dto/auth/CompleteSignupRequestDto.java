package com.pratham.livo.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompleteSignupRequestDto {
    private String registrationId;
    private String otp;
}
