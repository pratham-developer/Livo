package com.pratham.livo.service;

import com.pratham.livo.dto.auth.*;
import com.pratham.livo.dto.auth.otp.*;

public interface AuthService {
    LoginResponseDto login(LoginRequestDto loginRequestDto);
    void logout(String refreshToken);
    LoginResponseDto refresh(String refreshToken);

    InitiateOtpResponseDto initiateSignup(SignupRequestDto requestDto, String ipAddress);
    OtpVerifyResponseDto completeSignup(SignupOtpVerifyRequestDto requestDto, String ipAddress);
    ResendOtpResponseDto resendSignupOtp(ResendOtpRequestDto requestDto, String ipAddress);

    InitiateOtpResponseDto initiateForgotPwd(ForgotPwdRequestDto requestDto, String ipAddress);
    OtpVerifyResponseDto completeForgotPwd(ForgotOtpVerifyRequestDto requestDto, String ipAddress);
    ResendOtpResponseDto resendForgotPwdOtp(ResendOtpRequestDto requestDto, String ipAddress);


}
