package com.example.javalearn.order.domain.event;

import com.example.javalearn.order.domain.model.OrderId;
import com.example.javalearn.order.domain.model.Money;

public record OrderPaid(OrderId orderId, Money total) {
}
