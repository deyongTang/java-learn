package com.example.shardingdemo.repository;

import com.example.shardingdemo.domain.Order;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {

    private static final RowMapper<Order> ORDER_ROW_MAPPER = new RowMapper<Order>() {
        @Override
        public Order mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Order(
                    rs.getLong("order_id"),
                    rs.getLong("user_id"),
                    rs.getBigDecimal("amount"),
                    rs.getString("status"),
                    rs.getTimestamp("created_at").toLocalDateTime()
            );
        }
    };

    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(Order order) {
        String sql = "INSERT INTO t_order (order_id, user_id, amount, status, created_at) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                order.getOrderId(),
                order.getUserId(),
                order.getAmount(),
                order.getStatus(),
                Timestamp.valueOf(order.getCreatedAt())
        );
    }

    public Optional<Order> findById(long orderId) {
        String sql = "SELECT order_id, user_id, amount, status, created_at FROM t_order WHERE order_id = ?";
        try {
            Order order = jdbcTemplate.queryForObject(sql, ORDER_ROW_MAPPER, orderId);
            return Optional.ofNullable(order);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<Order> findByUserId(long userId, int limit) {
        String sql = "SELECT order_id, user_id, amount, status, created_at FROM t_order WHERE user_id = ? ORDER BY order_id DESC LIMIT ?";
        return jdbcTemplate.query(sql, ORDER_ROW_MAPPER, userId, limit);
    }
}
