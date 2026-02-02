package com.pratham.livo.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class WebConfig {

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
