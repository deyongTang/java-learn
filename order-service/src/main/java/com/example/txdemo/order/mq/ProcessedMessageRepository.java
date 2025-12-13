package com.example.txdemo.order.mq;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public class ProcessedMessageRepository {
    private final JdbcTemplate jdbcTemplate;

    public ProcessedMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean markProcessedOnce(String messageKey) {
        try {
            jdbcTemplate.update("""
                    insert into processed_messages (message_key, processed_at)
                    values (?, ?)
                    """, messageKey, Instant.now());
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }
}

