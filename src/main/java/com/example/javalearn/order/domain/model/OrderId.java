package com.example.javalearn.order.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class OrderId {
    private final String value;

    public OrderId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("orderId cannot be blank");
        }
        this.value = value;
    }

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderId orderId)) return false;
        return value.equals(orderId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
