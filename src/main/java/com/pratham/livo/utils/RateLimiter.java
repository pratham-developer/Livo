package com.pratham.livo.utils;

import com.pratham.livo.enums.HttpRequestType;
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

    //write config (strict)
    @Value("${livo.ratelimit.write.capacity}")
    private long writeCapacity;

    @Value("${livo.ratelimit.write.refill-tokens}")
    private long writeRefillTokens;

    @Value("${livo.ratelimit.write.refill-duration}")
    private long writeRefillDuration;

    //read config (lenient)
    @Value("${livo.ratelimit.read.capacity}")
    private long readCapacity;

    @Value("${livo.ratelimit.read.refill-tokens}")
    private long readRefillTokens;

    @Value("${livo.ratelimit.read.refill-duration}")
    private long readRefillDuration;

    @Value("${livo.ratelimit.enabled}")
    private boolean enabled;

    private final String keyPrefix = "ratelimit:";

    public ConsumptionProbe tryConsume(String ip, HttpRequestType httpRequestType){
        //if not enabled then show infinite tokens available
        if(!enabled){
            return ConsumptionProbe.consumed(Long.MAX_VALUE,0);
        }

        //set config
        long capacity = (httpRequestType == HttpRequestType.WRITE) ? writeCapacity : readCapacity;
        long refillTokens = (httpRequestType == HttpRequestType.WRITE) ? writeRefillTokens : readRefillTokens;
        long refillDuration = (httpRequestType == HttpRequestType.WRITE) ? writeRefillDuration : readRefillDuration;

        Supplier<BucketConfiguration> configSupplier = () ->
                BucketConfiguration.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(capacity)
                                .refillGreedy(refillTokens, Duration.ofSeconds(refillDuration))
                                .build())
                        .build();

        try {
            //try to consume token from the bucket
            String key = keyPrefix + httpRequestType.name() + ":" + ip;
            return proxyManager.builder().build(key,configSupplier)
                    .tryConsumeAndReturnRemaining(1);
        }catch (Exception e){
            log.error("Redis Rate Limiter failed for ip {}. Allowing request. Error: {}", ip, e.getMessage());
            return ConsumptionProbe.consumed(capacity, 0);
        }
    }


}
