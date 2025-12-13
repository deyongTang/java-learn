package com.example.txdemo.inventory.outbox;

public record OutboxRecord(long id, String aggregateId, String eventType, String payload) {}

