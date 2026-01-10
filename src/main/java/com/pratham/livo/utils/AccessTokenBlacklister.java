package com.pratham.livo.utils;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class AccessTokenBlacklister {

    @Value("${jwt.access.expiry}")
    private Long accessExpiry;

    private final String keyPrefix = "jwt:blacklist:";
    private final RedisTemplate<String, Object> redisTemplate;

    public void blacklist(String jti) {
        try {
            String key = keyPrefix + jti;
            redisTemplate.opsForValue()
                    .set(key, "1", accessExpiry, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Blacklist operation failed", e);
        }

    }

    public boolean isBlacklisted(String jti) {
        try {
            return redisTemplate.hasKey(keyPrefix + jti);
        } catch (Exception e) {
            throw new RuntimeException("Token blacklist check failed", e);
        }

    }

    public void blacklistBatch(List<String> jtis) {
        if (jtis == null || jtis.isEmpty()) {
            return;
        }

        try {
            redisTemplate.executePipelined(new SessionCallback<>() {
                @Override
                public Object execute(@NonNull RedisOperations operations) throws DataAccessException {
                    ValueOperations<String, String> ops = operations.opsForValue();
                    for (String jti : jtis) {
                        String key = keyPrefix + jti;
                        ops.set(key, "1", accessExpiry, TimeUnit.MILLISECONDS);
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Batch blacklist operation failed", e);
        }
    }
}
