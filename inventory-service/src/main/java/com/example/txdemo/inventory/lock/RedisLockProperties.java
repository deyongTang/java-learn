package com.example.txdemo.inventory.lock;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "txdemo.redis")
public record RedisLockProperties(String address) {}

