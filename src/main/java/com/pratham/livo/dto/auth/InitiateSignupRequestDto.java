package com.pratham.livo.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InitiateSignupRequestDto {
    private String name;
    private String email;
    private String password;
}
