package com.example.javalearn.order.domain.event;

import com.example.javalearn.order.domain.model.OrderId;
import com.example.javalearn.order.domain.model.Money;

public record OrderPlaced(OrderId orderId, Money total) {
}
