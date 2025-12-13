package com.example.txdemo.order.mq;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class RocketMqProducer {
    private final RocketMqProperties properties;
    private DefaultMQProducer producer;

    public RocketMqProducer(RocketMqProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void start() throws MQClientException {
        DefaultMQProducer p = new DefaultMQProducer(properties.producerGroup());
        p.setNamesrvAddr(properties.namesrv());
        p.start();
        this.producer = p;
    }

    public void send(String tag, String key, String jsonPayload) throws Exception {
        Message message = new Message(
                properties.topic(),
                tag,
                jsonPayload.getBytes(StandardCharsets.UTF_8)
        );
        message.setKeys(key);
        producer.send(message);
    }

    @PreDestroy
    public void shutdown() {
        if (producer != null) {
            producer.shutdown();
        }
    }
}

