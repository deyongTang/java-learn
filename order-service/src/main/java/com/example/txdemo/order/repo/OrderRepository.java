package com.example.txdemo.order.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class OrderRepository {
    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(String id, String productId, int quantity, String status, Instant createdAt) {
        jdbcTemplate.update("""
                insert into orders (id, product_id, quantity, status, created_at)
                values (?, ?, ?, ?, ?)
                """, id, productId, quantity, status, Timestamp.from(createdAt));
    }

    public void updateStatus(String id, String status) {
        jdbcTemplate.update("""
                update orders
                   set status = ?
                 where id = ?
                """, status, id);
    }

    public List<Map<String, Object>> findAll() {
        return jdbcTemplate.queryForList("""
                select id, product_id, quantity, status, created_at
                  from orders
                 order by created_at desc
                """);
    }

    public Optional<Map<String, Object>> findOne(String id) {
        return jdbcTemplate.query("""
                        select id, product_id, quantity, status, created_at
                          from orders
                         where id = ?
                        """,
                rs -> rs.next()
                        ? Optional.of(Map.of(
                        "id", rs.getString("id"),
                        "productId", rs.getString("product_id"),
                        "quantity", rs.getInt("quantity"),
                        "status", rs.getString("status"),
                        "createdAt", rs.getTimestamp("created_at").toInstant().toString()
                ))
                        : Optional.empty(),
                id
        );
    }
}

