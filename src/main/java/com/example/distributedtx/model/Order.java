package com.example.distributedtx.model;

import java.time.Instant;
import java.util.UUID;

public class Order {
    public enum Status {
        CREATED, RESERVED, CANCELLED
    }

    private final String id;
    private final String productId;
    private final int quantity;
    private final Instant createdAt;
    private Status status;

    public Order(String productId, int quantity) {
        this.id = UUID.randomUUID().toString();
        this.productId = productId;
        this.quantity = quantity;
        this.createdAt = Instant.now();
        this.status = Status.CREATED;
    }

    public String getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
