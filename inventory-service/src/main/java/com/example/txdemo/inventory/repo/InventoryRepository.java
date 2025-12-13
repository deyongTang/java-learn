package com.example.txdemo.inventory.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
public class InventoryRepository {
    private final JdbcTemplate jdbcTemplate;

    public InventoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(String productId, int available) {
        jdbcTemplate.update("""
                insert into inventory (product_id, available, reserved)
                values (?, ?, 0)
                on duplicate key update
                    available = values(available)
                """, productId, available);
    }

    public void reserve(String productId, int quantity) {
        int updated = jdbcTemplate.update("""
                update inventory
                   set available = available - ?,
                       reserved  = reserved + ?
                 where product_id = ?
                   and available >= ?
                """, quantity, quantity, productId, quantity);
        if (updated == 0) {
            throw new IllegalStateException("库存不足或商品不存在: " + productId);
        }
    }

    public void release(String productId, int quantity) {
        jdbcTemplate.update("""
                update inventory
                   set available = available + ?,
                       reserved  = reserved - ?
                 where product_id = ?
                """, quantity, quantity, productId);
    }

    public Optional<Map<String, Object>> findOne(String productId) {
        return jdbcTemplate.query("""
                        select product_id, available, reserved
                          from inventory
                         where product_id = ?
                        """,
                rs -> rs.next()
                        ? Optional.of(Map.of(
                        "productId", rs.getString("product_id"),
                        "available", rs.getInt("available"),
                        "reserved", rs.getInt("reserved")
                ))
                        : Optional.empty(),
                productId
        );
    }
}

