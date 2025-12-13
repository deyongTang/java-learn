package com.example.javalearn.order.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object for monetary amount (simple, no currency conversion).
 */
public final class Money {
    private final BigDecimal amount;

    public Money(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount cannot be null");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("amount cannot have more than 2 decimals");
        }
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money multiply(int qty) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(qty)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
