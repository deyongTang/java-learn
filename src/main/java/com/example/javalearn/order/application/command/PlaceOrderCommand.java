package com.example.javalearn.order.application.command;

import java.math.BigDecimal;
import java.util.List;

public record PlaceOrderCommand(String customerId, List<Item> items) {
    public record Item(String skuId, int quantity, BigDecimal unitPrice) {
    }
}
