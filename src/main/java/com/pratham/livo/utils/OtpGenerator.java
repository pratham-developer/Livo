package com.pratham.livo.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class OtpGenerator {

    private final int OTP_LENGTH = 6;
    private final int DIGIT_BOUND = 10;
    private final SecureRandom secureRandom;

    public String generate() {
        StringBuilder otp = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(secureRandom.nextInt(DIGIT_BOUND));
        }
        return otp.toString();
    }
}
