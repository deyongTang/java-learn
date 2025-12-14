package com.example.txdemo.inventory.mq;

import com.example.txdemo.inventory.outbox.OutboxRepository;
import com.example.txdemo.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class OrderCreatedHandler {
    private final InventoryService inventoryService;
    private final OutboxRepository outboxRepository;
    private final OutboxPublisher outboxPublisher;
    private final ObjectMapper objectMapper;

    public OrderCreatedHandler(
            InventoryService inventoryService,
            OutboxRepository outboxRepository,
            OutboxPublisher outboxPublisher,
            ObjectMapper objectMapper
    ) {
        this.inventoryService = inventoryService;
        this.outboxRepository = outboxRepository;
        this.outboxPublisher = outboxPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void handle(OrderCreatedEvent event) throws Exception {
        try {
            inventoryService.reserveWithLock(event.productId(), event.quantity());
            String payload = objectMapper.writeValueAsString(
                    new InventoryReservedEvent(event.orderId(), event.productId(), event.quantity())
            );
            outboxRepository.add(event.orderId(), EventType.INVENTORY_RESERVED, payload);
        } catch (Exception ex) {
            String payload = objectMapper.writeValueAsString(
                    new InventoryReserveFailedEvent(event.orderId(), event.productId(), event.quantity(), ex.getMessage())
            );
            outboxRepository.add(event.orderId(), EventType.INVENTORY_RESERVE_FAILED, payload);
        } finally {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        outboxPublisher.publishOnce();
                    }
                });
            } else {
                outboxPublisher.publishOnce();
            }
        }
    }
}
