package com.pratham.livo.service;

import com.pratham.livo.dto.auth.otp.OtpHelperDto;
import com.pratham.livo.dto.auth.otp.OtpSession;
import com.pratham.livo.enums.OtpType;

import java.util.Map;

public interface OtpService {

     OtpHelperDto createOtpSession(
            String email,
            OtpType otpType, Map<String,String> payload);

    OtpSession verifyOtp(String registrationId, OtpType otpType, String inputOtp);
    OtpHelperDto resendOtp(String registrationId, OtpType otpType);
    void deleteOtpSession(String registrationId, OtpSession otpSession);
}
