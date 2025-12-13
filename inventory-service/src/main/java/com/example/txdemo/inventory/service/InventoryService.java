package com.example.txdemo.inventory.service;

import com.example.txdemo.inventory.repo.InventoryRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final RedissonClient redissonClient;

    public InventoryService(InventoryRepository inventoryRepository, RedissonClient redissonClient) {
        this.inventoryRepository = inventoryRepository;
        this.redissonClient = redissonClient;
    }

    @Transactional
    public void seed(String productId, int available) {
        inventoryRepository.upsert(productId, available);
    }

    @Transactional
    public void reserveWithLock(String productId, int quantity) {
        String lockKey = "lock:inventory:" + productId;
        RLock lock = redissonClient.getLock(lockKey);

        boolean locked = false;
        try {
            locked = lock.tryLock(Duration.ofSeconds(2).toMillis(), Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);
            if (!locked) {
                throw new IllegalStateException("获取分布式锁失败: " + productId);
            }
            inventoryRepository.reserve(productId, quantity);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("获取分布式锁被中断: " + productId);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional
    public void release(String productId, int quantity) {
        inventoryRepository.release(productId, quantity);
    }

    public Optional<Map<String, Object>> get(String productId) {
        return inventoryRepository.findOne(productId);
    }
}

