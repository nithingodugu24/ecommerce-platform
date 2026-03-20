package com.nithingodugu.ecommerce.productservice.outbox.publisher;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OpenTelemetry openTelemetry;

    private static final TextMapSetter<ProducerRecord<String, String>> OTEL_SETTER =
            (record, key, value) ->
                    record.headers().add(key, value.getBytes(StandardCharsets.UTF_8));

    public CompletableFuture<SendResult<String, String>> publish(
            String topic,
            String key,
            String payload,
            String requestId
    ) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payload);

        if (requestId != null && !requestId.isBlank()) {
            record.headers().add("requestId", requestId.getBytes(StandardCharsets.UTF_8));
        }

        openTelemetry.getPropagators()
                .getTextMapPropagator()
                .inject(Context.current(), record, OTEL_SETTER);

        return kafkaTemplate.send(record).toCompletableFuture();
    }
}