package com.pratham.livo.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InitiateSignupResponseDto {
    private String registrationId;
    private long nextResendAt;
}
