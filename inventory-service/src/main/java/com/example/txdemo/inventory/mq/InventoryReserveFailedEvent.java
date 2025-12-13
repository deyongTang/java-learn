package com.example.txdemo.inventory.mq;

public record InventoryReserveFailedEvent(String orderId, String productId, int quantity, String reason) {}

