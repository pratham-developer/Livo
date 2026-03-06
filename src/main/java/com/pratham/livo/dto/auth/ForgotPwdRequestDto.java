package com.pratham.livo.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class ForgotPwdRequestDto {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    public void setEmail(String email) {
        if (email != null) {
            this.email = email.trim().toLowerCase();
        } else {
            this.email = null;
        }
    }
}