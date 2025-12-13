package com.example.txdemo.inventory.mq;

public final class EventType {
    private EventType() {}

    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String INVENTORY_RESERVED = "INVENTORY_RESERVED";
    public static final String INVENTORY_RESERVE_FAILED = "INVENTORY_RESERVE_FAILED";
}

