package com.example.javalearn.order.application.view;

import java.util.List;

public record OrderView(String id, String customerId, String status, String total, List<OrderItemView> items) {
    public record OrderItemView(String skuId, int quantity, String unitPrice, String lineTotal) {
    }
}
