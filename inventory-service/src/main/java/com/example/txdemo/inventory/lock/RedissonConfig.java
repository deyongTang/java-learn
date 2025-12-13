package com.example.txdemo.inventory.lock;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedisLockProperties properties) {
        Config config = new Config();
        config.useSingleServer().setAddress(properties.address());
        return Redisson.create(config);
    }
}

