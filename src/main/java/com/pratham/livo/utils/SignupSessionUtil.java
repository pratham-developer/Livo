package com.pratham.livo.utils;

import com.pratham.livo.dto.auth.SignupSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class SignupSessionUtil {
    @Value("${signup.expiry}")
    private Long signupExpiry;

    private final String keyPrefix = "signup:";
    private final RedisTemplate<String, Object> redisTemplate;

    public void pushToRedis(String registrationId, SignupSession signupSession) {
        try {
            String key = keyPrefix + registrationId;
            redisTemplate.opsForValue()
                    .set(key, signupSession, signupExpiry, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException("push to redis operation failed", e);
        }

    }

    public SignupSession fetchFromRedis(String registrationId) {
        try {
            String key = keyPrefix + registrationId;
            return (SignupSession) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            throw new RuntimeException("fetch from redis operation failed", e);
        }

    }

    public void deleteFromRedis(String registrationId) {
        try {
            String key = keyPrefix + registrationId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            throw new RuntimeException("delete from redis operation failed", e);
        }
    }

    public boolean updateInRedis(String registrationId, SignupSession signupSession) {
        try {
            String key = keyPrefix + registrationId;
            Long remainingTtl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (remainingTtl != null && remainingTtl > 0) {
                redisTemplate.opsForValue().set(key, signupSession, remainingTtl, TimeUnit.SECONDS);
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("update in redis operation failed", e);
        }
    }
}
