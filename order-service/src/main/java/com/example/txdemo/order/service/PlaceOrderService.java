package com.example.txdemo.order.service;

import com.example.txdemo.order.mq.EventType;
import com.example.txdemo.order.mq.OrderCreatedEvent;
import com.example.txdemo.order.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceOrderService {
    private final OrderService orderService;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public PlaceOrderService(OrderService orderService, OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public String place(String productId, int quantity) throws Exception {
        String orderId = orderService.createPending(productId, quantity);
        String payload = objectMapper.writeValueAsString(new OrderCreatedEvent(orderId, productId, quantity));
        outboxRepository.add(orderId, EventType.ORDER_CREATED, payload);
        return orderId;
    }
}

