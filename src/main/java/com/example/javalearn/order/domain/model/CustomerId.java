package com.example.javalearn.order.domain.model;

import java.util.Objects;

public final class CustomerId {
    private final String value;

    public CustomerId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("customerId cannot be blank");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomerId that)) return false;
        return value.equals(that.value);
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
