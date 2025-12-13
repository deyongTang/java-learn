package com.example.distributedtx.model;

public class InventoryItem {
    private final String productId;
    private int available;

    public InventoryItem(String productId, int available) {
        this.productId = productId;
        this.available = available;
    }

    public String getProductId() {
        return productId;
    }

    public int getAvailable() {
        return available;
    }

    public void reserve(int quantity) {
        if (quantity > available) {
            throw new IllegalStateException("库存不足，无法预留" + quantity + "件");
        }
        available -= quantity;
    }

    public void release(int quantity) {
        available += quantity;
    }
}
