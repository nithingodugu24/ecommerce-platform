package com.nithingodugu.ecommerce.paymentservice.outbox.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public CompletableFuture<SendResult<String, String>> publish(
            String topic, String key, String payload
    ) {
        return kafkaTemplate.send(topic, key, payload).toCompletableFuture();
    }

}
