package com.example.distributedtx.service;

import com.example.distributedtx.model.InventoryItem;

import java.util.HashMap;
import java.util.Map;

public class InventoryService {
    private final Map<String, InventoryItem> inventory = new HashMap<>();

    public void seed(String productId, int available) {
        inventory.put(productId, new InventoryItem(productId, available));
    }

    public void reserve(String productId, int quantity) {
        InventoryItem item = inventory.computeIfAbsent(productId, id -> new InventoryItem(id, 0));
        item.reserve(quantity);
        System.out.printf("[库存服务] 预留商品 %s 数量 %d，剩余 %d\n", productId, quantity, item.getAvailable());
    }

    public void release(String productId, int quantity) {
        InventoryItem item = inventory.get(productId);
        if (item == null) {
            return;
        }
        item.release(quantity);
        System.out.printf("[库存服务] 补偿释放商品 %s 数量 %d，剩余 %d\n", productId, quantity, item.getAvailable());
    }

    public Map<String, InventoryItem> getInventory() {
        return inventory;
    }
}
