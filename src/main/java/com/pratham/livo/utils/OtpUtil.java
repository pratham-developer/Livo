package com.pratham.livo.utils;

import com.pratham.livo.dto.auth.otp.OtpSession;
import com.pratham.livo.enums.OtpType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OtpUtil {
    @Value("${signup.expiry}")
    private Long sessionExpiry;

    private final String keyPrefixSignup = "signup:";
    private final String keyPrefixForgot = "forgot:";
    private final int OTP_LENGTH = 6;
    private final int DIGIT_BOUND = 10;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SecureRandom secureRandom;

    public String generateOtp() {
        StringBuilder otp = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(secureRandom.nextInt(DIGIT_BOUND));
        }
        return otp.toString();
    }

    private String getKey(String registrationId, OtpType otpType){
        if(OtpType.SIGNUP.equals(otpType)){
            return keyPrefixSignup + registrationId;
        }
        else if(OtpType.FORGOT.equals(otpType)){
            return keyPrefixForgot + registrationId;
        }
        throw new RuntimeException("invalid otp type");
    }

    public void pushToRedis(String registrationId, OtpSession otpSession) {
        try {
            String key = getKey(registrationId, otpSession.getOtpType());
            redisTemplate.opsForValue()
                    .set(key, otpSession, sessionExpiry, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException("push to redis operation failed", e);
        }

    }

    public OtpSession fetchFromRedis(String registrationId, OtpType otpType) {
        try {
            String key = getKey(registrationId,otpType);
            return (OtpSession) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            throw new RuntimeException("fetch from redis operation failed", e);
        }

    }

    public void deleteFromRedis(String registrationId, OtpType otpType) {
        try {
            String key = getKey(registrationId, otpType);
            redisTemplate.delete(key);
        } catch (Exception e) {
            throw new RuntimeException("delete from redis operation failed", e);
        }
    }

    public boolean updateInRedis(String registrationId, OtpSession otpSession) {
        try {
            String key = getKey(registrationId,otpSession.getOtpType());
            Long remainingTtl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (remainingTtl != null && remainingTtl > 0) {
                redisTemplate.opsForValue().set(key, otpSession, remainingTtl, TimeUnit.SECONDS);
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("update in redis operation failed", e);
        }
    }
}
