package com.example.javalearn.order.domain.repository;

import com.example.javalearn.order.domain.model.Order;
import com.example.javalearn.order.domain.model.OrderId;

import java.util.Optional;

public interface OrderRepository {
    Optional<Order> find(OrderId id);

    void save(Order order);
}
