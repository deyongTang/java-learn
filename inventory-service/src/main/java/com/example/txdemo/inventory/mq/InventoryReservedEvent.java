package com.example.txdemo.inventory.mq;

public record InventoryReservedEvent(String orderId, String productId, int quantity) {}

