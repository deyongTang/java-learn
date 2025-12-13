package com.example.txdemo.order.mq;

import com.example.txdemo.order.service.OrderService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryEventConsumer {
    private final RocketMqProperties properties;
    private final OrderService orderService;
    private final ProcessedMessageRepository processedMessageRepository;

    private DefaultMQPushConsumer consumer;

    public InventoryEventConsumer(
            RocketMqProperties properties,
            OrderService orderService,
            ProcessedMessageRepository processedMessageRepository
    ) {
        this.properties = properties;
        this.orderService = orderService;
        this.processedMessageRepository = processedMessageRepository;
    }

    @PostConstruct
    public void start() throws MQClientException {
        DefaultMQPushConsumer c = new DefaultMQPushConsumer(properties.consumerGroup());
        c.setNamesrvAddr(properties.namesrv());
        c.subscribe(properties.topic(), String.join("||", EventType.INVENTORY_RESERVED, EventType.INVENTORY_RESERVE_FAILED));
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

            if (EventType.INVENTORY_RESERVED.equals(tag)) {
                orderService.confirm(orderId);
            } else if (EventType.INVENTORY_RESERVE_FAILED.equals(tag)) {
                orderService.cancel(orderId);
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

