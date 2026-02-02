package com.pratham.livo.security;

import com.pratham.livo.exception.RateLimitExceededException;
import com.pratham.livo.utils.IpUtil;
import com.pratham.livo.utils.RateLimiter;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {
    private final IpUtil ipUtil;
    private final RateLimiter rateLimiter;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        //get ip
        String ip = ipUtil.getClientIp(request);

        //try to consume token from the bucket for the ip
        ConsumptionProbe consumptionProbe = rateLimiter.tryConsume(ip);
        response.setHeader("X-RateLimit-Remaining",String.valueOf(consumptionProbe.getRemainingTokens()));

        if(consumptionProbe.isConsumed()){
            //if token consumed then allow request
            filterChain.doFilter(request,response);
        }else {
            //else throw exception
            long nanos = consumptionProbe.getNanosToWaitForRefill();
            long waitForRefill = (long) Math.ceil(nanos / 1_000_000_000.0);
            waitForRefill = waitForRefill <= 0 ? 1 : waitForRefill;
            handlerExceptionResolver.resolveException(
                    request,response,null,
                    new RateLimitExceededException("Rate limit exceeded",waitForRefill)
            );
        }

    }
}
