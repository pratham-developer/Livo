package com.pratham.livo.service;

import com.pratham.livo.dto.auth.otp.OtpHelperDto;
import com.pratham.livo.dto.auth.otp.OtpSession;
import com.pratham.livo.enums.OtpType;

import java.util.Map;

public interface OtpService {

     OtpHelperDto createOtpSession(
            String email, String ipAddress,
            OtpType otpType, Map<String,String> payload);

    OtpSession verifyOtp(String registrationId, String ipAddress, OtpType otpType, String inputOtp);
    OtpHelperDto resendOtp(String registrationId, String ipAddress, OtpType otpType);
    void deleteOtpSession(String registrationId, OtpSession otpSession);
}
