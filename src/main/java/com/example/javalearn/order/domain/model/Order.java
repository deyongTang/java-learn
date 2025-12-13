package com.example.javalearn.order.domain.model;

import com.example.javalearn.order.domain.event.OrderPaid;
import com.example.javalearn.order.domain.event.OrderPlaced;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate root enforcing invariants on items and lifecycle (pay/cancel).
 */
public class Order {
    private final OrderId id;
    private final CustomerId customerId;
    private final List<OrderItem> items;
    private final Money total;
    private OrderStatus status;
    private final List<Object> pendingEvents = new ArrayList<>();

    private Order(OrderId id, CustomerId customerId, List<OrderItem> items, Money total, OrderStatus status) {
        this.id = id;
        this.customerId = customerId;
        this.items = items;
        this.total = total;
        this.status = status;
    }

    public static Order place(OrderId id, CustomerId customerId, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("order must have at least one item");
        }
        Money computed = items.stream()
                .map(OrderItem::lineTotal)
                .reduce(new Money(java.math.BigDecimal.ZERO), Money::add);
        Order order = new Order(id, customerId, List.copyOf(items), computed, OrderStatus.CREATED);
        order.pendingEvents.add(new OrderPlaced(id, computed));
        return order;
    }

    public OrderPaid pay() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("only newly created orders can be paid");
        }
        status = OrderStatus.PAID;
        OrderPaid paid = new OrderPaid(id, total);
        pendingEvents.add(paid);
        return paid;
    }

    public void cancel() {
        if (status == OrderStatus.PAID) {
            throw new IllegalStateException("paid order cannot be canceled");
        }
        status = OrderStatus.CANCELED;
    }

    public OrderId getId() {
        return id;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Money getTotal() {
        return total;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<Object> getPendingEvents() {
        return Collections.unmodifiableList(pendingEvents);
    }

    public void clearPendingEvents() {
        pendingEvents.clear();
    }
}
