package com.example.txdemo.order.outbox;

public record OutboxRecord(long id, String aggregateId, String eventType, String payload) {}

