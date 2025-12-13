package com.example.txdemo.order.mq;

public record OrderCreatedEvent(String orderId, String productId, int quantity) {}

