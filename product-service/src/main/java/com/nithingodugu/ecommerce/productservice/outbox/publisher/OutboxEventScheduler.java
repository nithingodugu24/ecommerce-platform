package com.nithingodugu.ecommerce.productservice.outbox.publisher;

import com.nithingodugu.ecommerce.productservice.outbox.entity.OutboxEvent;
import com.nithingodugu.ecommerce.productservice.outbox.entity.OutboxStatus;
import com.nithingodugu.ecommerce.productservice.outbox.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaEventPublisher kafkaEventPublisher;
    private static final int MAX_RETRY = 5;

    @Scheduled(fixedDelay = 5000)
    public void publishOutboxEvents()  {

        List<OutboxEvent> events = outboxEventRepository.findTop100PendingEvents(OutboxStatus.PENDING);

        for(OutboxEvent event: events){
            kafkaEventPublisher.publish(
                    event.getTopic(),
                    event.getAggregateId(),
                    event.getPayload()
            ).whenComplete((result, ex)->{
                if (ex == null){
                    markProcessed(event.getId());
                }else{
                    markFailed(event.getId(), ex);
                }
            });
        }
    }

    @Transactional
    public void markProcessed(Long outboxId) {
        outboxEventRepository.findById(outboxId).ifPresent(event -> {
            event.setStatus(OutboxStatus.PROCESSED);
            event.setProcessedAt(Instant.now());
            outboxEventRepository.save(event);
        });
    }

    @Transactional
    public void markFailed(Long outboxId, Throwable ex) {
        outboxEventRepository.findById(outboxId).ifPresent(event -> {
            int retries = event.getRetryCount() + 1;
            event.setRetryCount(retries);
            if (retries >= MAX_RETRY) {
                event.setStatus(OutboxStatus.FAILED);
                log.error("Outbox event permanently failed eventId={}", event.getEventId(), ex);
            }
            outboxEventRepository.save(event);
        });
    }


}
