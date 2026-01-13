package com.pratham.livo.service;

import com.pratham.livo.dto.auth.*;

public interface AuthService {
    LoginResponseDto login(LoginRequestDto loginRequestDto);
    void logout(String refreshToken);
    LoginResponseDto refresh(String refreshToken);

    InitiateSignupResponseDto initiateSignup(InitiateSignupRequestDto requestDto, String ipAddress);
    CompleteSignupResponseDto completeSignup(CompleteSignupRequestDto requestDto, String ipAddress);
    ResendOtpResponseDto resendSignupOtp(ResendOtpRequestDto requestDto, String ipAddress);

}
