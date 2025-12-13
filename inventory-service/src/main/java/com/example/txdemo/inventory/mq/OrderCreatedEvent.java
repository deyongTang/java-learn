package com.example.txdemo.inventory.mq;

public record OrderCreatedEvent(String orderId, String productId, int quantity) {}

