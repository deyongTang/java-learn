package com.example.javalearn.order.application;

import com.example.javalearn.order.application.command.PlaceOrderCommand;
import com.example.javalearn.order.application.view.OrderView;
import com.example.javalearn.order.domain.model.CustomerId;
import com.example.javalearn.order.domain.model.Money;
import com.example.javalearn.order.domain.model.Order;
import com.example.javalearn.order.domain.model.OrderId;
import com.example.javalearn.order.domain.model.OrderItem;
import com.example.javalearn.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderAppService {

    private final OrderRepository orderRepository;

    public OrderAppService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderId place(PlaceOrderCommand command) {
        List<OrderItem> items = command.items().stream()
                .map(item -> new OrderItem(item.skuId(), item.quantity(), new Money(item.unitPrice())))
                .toList();
        Order order = Order.place(OrderId.newId(), new CustomerId(command.customerId()), items);
        orderRepository.save(order);
        // In a real app, publish order events here.
        order.clearPendingEvents();
        return order.getId();
    }

    @Transactional
    public void pay(String orderId) {
        Order order = orderRepository.find(new OrderId(orderId))
                .orElseThrow(() -> new IllegalArgumentException("order not found"));
        order.pay();
        orderRepository.save(order);
        order.clearPendingEvents();
    }

    @Transactional(readOnly = true)
    public OrderView get(String orderId) {
        Order order = orderRepository.find(new OrderId(orderId))
                .orElseThrow(() -> new IllegalArgumentException("order not found"));
        return toView(order);
    }

    private OrderView toView(Order order) {
        List<OrderView.OrderItemView> views = order.getItems().stream()
                .map(item -> new OrderView.OrderItemView(
                        item.getSkuId(),
                        item.getQuantity(),
                        item.getUnitPrice().toString(),
                        item.lineTotal().toString()
                ))
                .collect(Collectors.toUnmodifiableList());
        return new OrderView(
                order.getId().getValue(),
                order.getCustomerId().getValue(),
                order.getStatus().name(),
                order.getTotal().toString(),
                views
        );
    }
}
