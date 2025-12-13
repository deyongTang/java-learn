package com.example.javalearn.order.infrastructure.persistence;

import com.example.javalearn.order.domain.model.Order;
import com.example.javalearn.order.domain.model.OrderId;
import com.example.javalearn.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<Order> find(OrderId id) {
        return Optional.ofNullable(storage.get(id.getValue()));
    }

    @Override
    public void save(Order order) {
        storage.put(order.getId().getValue(), order);
    }

    public void put(Order order) {
        save(order);
    }
}
