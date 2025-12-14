package com.example.txdemo.inventory.mq;

import com.example.txdemo.inventory.outbox.OutboxRecord;
import com.example.txdemo.inventory.outbox.OutboxRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

@Component
public class OutboxPublisher {
    private final OutboxRepository outboxRepository;
    private final RocketMqProducer producer;
    private final ReentrantLock publishLock = new ReentrantLock();

    public OutboxPublisher(OutboxRepository outboxRepository, RocketMqProducer producer) {
        this.outboxRepository = outboxRepository;
        this.producer = producer;
    }

    @Scheduled(fixedDelay = 500)
    public void publish() {
        publishOnce();
    }

    public void publishOnce() {
        if (!publishLock.tryLock()) {
            return;
        }
        try {
            for (OutboxRecord record : outboxRepository.fetchNew(50)) {
                try {
                    producer.send(record.eventType(), record.aggregateId(), record.payload());
                    outboxRepository.markSent(record.id());
                } catch (Exception ex) {
                    outboxRepository.markFailed(record.id(), ex.getMessage());
                }
            }
        } finally {
            publishLock.unlock();
        }
    }
}
