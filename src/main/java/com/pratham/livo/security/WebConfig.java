package com.pratham.livo.security;

import com.pratham.livo.utils.IpUtil;
import com.pratham.livo.utils.RateLimiter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
public class WebConfig {

    @Bean
    public RateLimitFilter rateLimitFilter(
            IpUtil ipUtil,
            RateLimiter rateLimiter,
            HandlerExceptionResolver handlerExceptionResolver
    ) {
        return new RateLimitFilter(ipUtil, rateLimiter, handlerExceptionResolver);
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterFilterRegistrationBean(
            RateLimitFilter rateLimitFilter
    ){
        FilterRegistrationBean<RateLimitFilter> filterRegistrationBean =
                new FilterRegistrationBean<>(rateLimitFilter);

        filterRegistrationBean.addUrlPatterns("/*");
        //register at the highest level
        filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return filterRegistrationBean;
    }
}
