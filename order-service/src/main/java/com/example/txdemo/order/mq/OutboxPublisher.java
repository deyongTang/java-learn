package com.example.txdemo.order.mq;

import com.example.txdemo.order.outbox.OutboxRecord;
import com.example.txdemo.order.outbox.OutboxRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {
    private final OutboxRepository outboxRepository;
    private final RocketMqProducer producer;

    public OutboxPublisher(OutboxRepository outboxRepository, RocketMqProducer producer) {
        this.outboxRepository = outboxRepository;
        this.producer = producer;
    }

    @Scheduled(fixedDelay = 500)
    public void publish() {
        for (OutboxRecord record : outboxRepository.fetchNew(50)) {
            try {
                producer.send(record.eventType(), record.aggregateId(), record.payload());
                outboxRepository.markSent(record.id());
            } catch (Exception ex) {
                outboxRepository.markFailed(record.id(), ex.getMessage());
            }
        }
    }
}

