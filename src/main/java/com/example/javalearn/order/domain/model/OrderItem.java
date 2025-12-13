package com.example.javalearn.order.domain.model;

import java.util.Objects;

public final class OrderItem {
    private final String skuId;
    private final int quantity;
    private final Money unitPrice;

    public OrderItem(String skuId, int quantity, Money unitPrice) {
        if (skuId == null || skuId.isBlank()) {
            throw new IllegalArgumentException("skuId cannot be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        this.skuId = skuId;
        this.quantity = quantity;
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice");
    }

    public String getSkuId() {
        return skuId;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public Money lineTotal() {
        return unitPrice.multiply(quantity);
    }
}
