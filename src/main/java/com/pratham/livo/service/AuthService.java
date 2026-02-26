package com.pratham.livo.service;

import com.pratham.livo.dto.auth.*;
import com.pratham.livo.dto.auth.otp.*;

public interface AuthService {
    LoginResponseDto login(LoginRequestDto loginRequestDto);
    void logout(String refreshToken);
    LoginResponseDto refresh(String refreshToken);

    InitiateOtpResponseDto initiateSignup(SignupRequestDto requestDto);
    OtpVerifyResponseDto completeSignup(SignupOtpVerifyRequestDto requestDto);
    ResendOtpResponseDto resendSignupOtp(ResendOtpRequestDto requestDto);

    InitiateOtpResponseDto initiateForgotPwd(ForgotPwdRequestDto requestDto);
    OtpVerifyResponseDto completeForgotPwd(ForgotOtpVerifyRequestDto requestDto);
    ResendOtpResponseDto resendForgotPwdOtp(ResendOtpRequestDto requestDto);


}
