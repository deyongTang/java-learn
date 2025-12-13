package com.example.txdemo.inventory.outbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class OutboxRepository {
    private final JdbcTemplate jdbcTemplate;

    public OutboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void add(String aggregateId, String eventType, String payload) {
        jdbcTemplate.update("""
                insert into outbox (aggregate_id, event_type, payload, status, created_at)
                values (?, ?, ?, 'NEW', ?)
                """, aggregateId, eventType, payload, Instant.now());
    }

    public List<OutboxRecord> fetchNew(int limit) {
        return jdbcTemplate.query("""
                        select id, aggregate_id, event_type, payload
                          from outbox
                         where status = 'NEW'
                         order by id
                         limit ?
                        """,
                (rs, rowNum) -> new OutboxRecord(
                        rs.getLong("id"),
                        rs.getString("aggregate_id"),
                        rs.getString("event_type"),
                        rs.getString("payload")
                ),
                limit
        );
    }

    public void markSent(long id) {
        jdbcTemplate.update("""
                update outbox
                   set status = 'SENT',
                       sent_at = ?
                 where id = ?
                """, Instant.now(), id);
    }

    public void markFailed(long id, String error) {
        jdbcTemplate.update("""
                update outbox
                   set retries = retries + 1,
                       error = ?
                 where id = ?
                """, error, id);
    }
}

