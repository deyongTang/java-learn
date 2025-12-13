package com.example.distributedtx.service;

import com.example.distributedtx.model.Order;

import java.util.HashMap;
import java.util.Map;

public class OrderService {
    private final Map<String, Order> orders = new HashMap<>();

    public Order createOrder(String productId, int quantity) {
        Order order = new Order(productId, quantity);
        orders.put(order.getId(), order);
        System.out.printf("[订单服务] 创建订单 %s，商品 %s，数量 %d\n", order.getId(), productId, quantity);
        return order;
    }

    public void markReserved(Order order) {
        order.setStatus(Order.Status.RESERVED);
        System.out.printf("[订单服务] 订单 %s 已进入 RESERVED 状态\n", order.getId());
    }

    public void cancelOrder(Order order) {
        order.setStatus(Order.Status.CANCELLED);
        System.out.printf("[订单服务] 订单 %s 被补偿取消\n", order.getId());
    }

    public Map<String, Order> getOrders() {
        return orders;
    }
}
