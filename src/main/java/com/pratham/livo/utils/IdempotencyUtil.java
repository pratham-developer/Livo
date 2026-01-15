package com.pratham.livo.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class IdempotencyUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    public boolean acquireLock(String key, Long ttlInMinutes) {
        try {
           Boolean success = redisTemplate.opsForValue()
                   .setIfAbsent(key,"1",ttlInMinutes,TimeUnit.MINUTES);
           return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            throw new RuntimeException("Redis lock failed for idempotency key", e);
        }
    }

}
