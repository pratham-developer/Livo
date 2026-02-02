package com.pratham.livo.utils;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimiter {

    private final ProxyManager<String> proxyManager;

    @Value("${livo.ratelimit.capacity}")
    private long capacity;

    @Value("${livo.ratelimit.refill-tokens}")
    private long refillTokens;

    @Value("${livo.ratelimit.refill-duration}")
    private long refillDuration;

    @Value("${livo.ratelimit.enabled}")
    private boolean enabled;

    private final String keyPrefix = "ratelimit:";

    public ConsumptionProbe tryConsume(String ip){
        //if not enabled then show infinite tokens available
        if(!enabled){
            return ConsumptionProbe.consumed(Long.MAX_VALUE,0);
        }

        Supplier<BucketConfiguration> configSupplier = () ->
                BucketConfiguration.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(capacity)
                                .refillGreedy(refillTokens, Duration.ofSeconds(refillDuration))
                                .build())
                        .build();

        try {
            //try to consume token from the bucket
            String key = keyPrefix + ip;
            return proxyManager.builder().build(key,configSupplier)
                    .tryConsumeAndReturnRemaining(1);
        }catch (Exception e){
            log.error("Redis Rate Limiter failed for ip {}. Allowing request. Error: {}", ip, e.getMessage());
            return ConsumptionProbe.consumed(capacity, 0);
        }
    }


}
