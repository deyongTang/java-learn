package com.example.shardingdemo.service;

import com.example.shardingdemo.domain.Order;
import com.example.shardingdemo.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final AtomicLong idGenerator = new AtomicLong(1000);

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order createOrder(Long orderId, Long userId, BigDecimal amount) {
        long finalOrderId = orderId != null ? orderId : idGenerator.incrementAndGet();
        Order order = new Order(finalOrderId, userId, amount, "CREATED", LocalDateTime.now());
        orderRepository.insert(order);
        return order;
    }

    public Optional<Order> findById(long orderId) {
        return orderRepository.findById(orderId);
    }

    public List<Order> findByUserId(long userId, int limit) {
        return orderRepository.findByUserId(userId, limit);
    }
}
