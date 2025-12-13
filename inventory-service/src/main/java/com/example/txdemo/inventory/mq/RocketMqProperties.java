package com.example.txdemo.inventory.mq;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "txdemo.rocketmq")
public record RocketMqProperties(
        String namesrv,
        String topic,
        String producerGroup,
        String consumerGroup
) {}

