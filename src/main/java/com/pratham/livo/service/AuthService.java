package com.pratham.livo.service;

import com.pratham.livo.dto.auth.LoginRequestDto;
import com.pratham.livo.dto.auth.LoginResponseDto;
import com.pratham.livo.dto.auth.SignupRequestDto;
import com.pratham.livo.dto.auth.SignupResponseDto;

public interface AuthService {
    SignupResponseDto signup(SignupRequestDto signupRequestDto);
    LoginResponseDto login(LoginRequestDto loginRequestDto);
    void logout(String refreshToken);
    LoginResponseDto refresh(String refreshToken);
}
