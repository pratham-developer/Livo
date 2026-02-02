package com.pratham.livo.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.username:#{null}}")
    private String username;

    @Value("${spring.data.redis.password}")
    private String password;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

    @Bean(destroyMethod = "shutdown")
    //client used for rate limiting
    public RedissonClient redissonClient(){
        Config config = new Config();
        String prefix = sslEnabled ? "rediss://" : "redis://" ;
        config.useSingleServer()
                .setAddress(prefix+host+":"+port)
                .setConnectionPoolSize(64)
                .setConnectionMinimumIdleSize(24);

        if (password != null && !password.isBlank()) {
            config.setPassword(password);
            if (username != null && !username.isBlank()) {
                config.setUsername(username);
            }
        }

        return Redisson.create(config);
    }
}
