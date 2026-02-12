package com.pratham.livo.dto.auth.otp;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResendOtpRequestDto {
    @NotBlank(message = "Registration ID is required")
    private String registrationId;
}
