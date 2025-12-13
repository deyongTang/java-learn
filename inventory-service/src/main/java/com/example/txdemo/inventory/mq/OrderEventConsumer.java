package com.example.txdemo.inventory.mq;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class OrderEventConsumer {
    private final RocketMqProperties properties;
    private final ProcessedMessageRepository processedMessageRepository;
    private final OrderCreatedHandler handler;
    private final ObjectMapper objectMapper;

    private DefaultMQPushConsumer consumer;

    public OrderEventConsumer(
            RocketMqProperties properties,
            ProcessedMessageRepository processedMessageRepository,
            OrderCreatedHandler handler,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.processedMessageRepository = processedMessageRepository;
        this.handler = handler;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void start() throws MQClientException {
        DefaultMQPushConsumer c = new DefaultMQPushConsumer(properties.consumerGroup());
        c.setNamesrvAddr(properties.namesrv());
        c.subscribe(properties.topic(), EventType.ORDER_CREATED);
        c.registerMessageListener((MessageListenerConcurrently) this::consume);
        c.start();
        this.consumer = c;
    }

    private ConsumeConcurrentlyStatus consume(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        for (MessageExt msg : msgs) {
            String orderId = msg.getKeys();
            String tag = msg.getTags();
            String messageKey = tag + ":" + orderId;

            if (!processedMessageRepository.markProcessedOnce(messageKey)) {
                continue;
            }

            try {
                String json = new String(msg.getBody(), StandardCharsets.UTF_8);
                OrderCreatedEvent event = objectMapper.readValue(json, OrderCreatedEvent.class);
                handler.handle(event);
            } catch (Exception ex) {
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    @PreDestroy
    public void shutdown() {
        if (consumer != null) {
            consumer.shutdown();
        }
    }
}
