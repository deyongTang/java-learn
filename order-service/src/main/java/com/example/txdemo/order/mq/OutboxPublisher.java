package com.example.txdemo.order.mq;

import com.example.txdemo.order.outbox.OutboxRecord;
import com.example.txdemo.order.outbox.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
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
                    log.error("Error while sending event", ex);
                    outboxRepository.markFailed(record.id(), ex.getMessage());
                }
            }
        } finally {
            publishLock.unlock();
        }
    }
}
