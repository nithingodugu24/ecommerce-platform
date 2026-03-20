package com.nithingodugu.ecommerce.productservice.scheduler;

import com.nithingodugu.ecommerce.productservice.outbox.entity.OutboxEvent;
import com.nithingodugu.ecommerce.productservice.outbox.entity.OutboxStatus;
import com.nithingodugu.ecommerce.productservice.outbox.publisher.KafkaEventPublisher;
import com.nithingodugu.ecommerce.productservice.outbox.repository.OutboxEventRepository;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final OpenTelemetry openTelemetry;
    private static final int MAX_RETRY = 5;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishOutboxEvents() {

        List<OutboxEvent> events = outboxEventRepository
                .findTop100PendingEvents(OutboxStatus.PENDING);

        if (events.isEmpty()) return;

        for (OutboxEvent event : events) {

            event.setStatus(OutboxStatus.PROCESSING);
            outboxEventRepository.save(event);

            var tracer = openTelemetry.getTracer("outbox-processor");

            var spanBuilder = tracer.spanBuilder("outbox.publish")
                    .setSpanKind(SpanKind.PRODUCER)
                    .setAttribute("messaging.system", "kafka")
                    .setAttribute("messaging.destination", event.getTopic())
                    .setAttribute("requestId", event.getRequestId());

            if (event.getOriginalTraceId() != null && event.getOriginalSpanId() != null) {
                spanBuilder.addLink(SpanContext.createFromRemoteParent(
                        event.getOriginalTraceId(),
                        event.getOriginalSpanId(),
                        TraceFlags.getSampled(),
                        TraceState.getDefault()
                ));
            }

            var span = spanBuilder.startSpan();

            final var publishSpan = span;
            final Long eventId = event.getId();
            final String eventLogId = event.getEventId();

            try (var scope = span.makeCurrent()) {

                kafkaEventPublisher.publish(
                        event.getTopic(),
                        event.getAggregateId(),
                        event.getPayload(),
                        event.getRequestId()
                ).whenComplete((result, ex) -> {
                    try {
                        if (ex == null) {
                            markProcessed(eventId);
                        } else {
                            publishSpan.recordException(ex);
                            markFailed(eventId, ex);
                        }
                    } finally {
                        publishSpan.end();
                    }
                });

            } catch (Exception ex) {
                span.recordException(ex);
                span.end();
                markFailed(eventId, ex);
            }
        }
    }

    @Transactional
    public void markProcessed(Long outboxId) {
        outboxEventRepository.findById(outboxId).ifPresent(e -> {
            e.setStatus(OutboxStatus.PROCESSED);
            e.setProcessedAt(Instant.now());
            outboxEventRepository.save(e);
        });
    }

    @Transactional
    public void markFailed(Long outboxId, Throwable ex) {
        outboxEventRepository.findById(outboxId).ifPresent(e -> {
            int retries = e.getRetryCount() + 1;
            e.setRetryCount(retries);
            if (retries >= MAX_RETRY) {
                e.setStatus(OutboxStatus.FAILED);
                log.error("Outbox event permanently failed eventId={}", e.getEventId(), ex);
            } else {
                e.setStatus(OutboxStatus.PENDING);
            }
            outboxEventRepository.save(e);
        });
    }
}