package com.example.txdemo.order.service;

import com.example.txdemo.order.domain.OrderStatus;
import com.example.txdemo.order.repo.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public String createPending(String productId, int quantity) {
        String orderId = UUID.randomUUID().toString();
        orderRepository.insert(orderId, productId, quantity, OrderStatus.PENDING, Instant.now());
        return orderId;
    }

    @Transactional
    public void confirm(String orderId) {
        orderRepository.updateStatus(orderId, OrderStatus.CONFIRMED);
    }

    @Transactional
    public void cancel(String orderId) {
        orderRepository.updateStatus(orderId, OrderStatus.CANCELLED);
    }

    public List<Map<String, Object>> listOrders() {
        return orderRepository.findAll();
    }

    public Optional<Map<String, Object>> getOrder(String orderId) {
        return orderRepository.findOne(orderId);
    }
}

