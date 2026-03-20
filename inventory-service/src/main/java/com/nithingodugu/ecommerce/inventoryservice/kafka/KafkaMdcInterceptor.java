package com.nithingodugu.ecommerce.inventoryservice.kafka;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.stream.StreamSupport;

@Component
@RequiredArgsConstructor
public class KafkaMdcInterceptor implements RecordInterceptor<String, String> {

    private static final String REQUEST_ID = "requestId";
    private static final ThreadLocal<Scope> SCOPE_HOLDER = new ThreadLocal<>();

    private static final TextMapGetter<ConsumerRecord<?, ?>> OTEL_GETTER =
            new TextMapGetter<>() {
                @Override
                public Iterable<String> keys(ConsumerRecord<?, ?> record) {
                    return () -> StreamSupport
                            .stream(record.headers().spliterator(), false)
                            .map(Header::key)
                            .iterator();
                }

                @Override
                public String get(ConsumerRecord<?, ?> record, String key) {
                    Header header = record.headers().lastHeader(key);
                    return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
                }
            };

    private final OpenTelemetry openTelemetry;

    @Override
    public ConsumerRecord<String, String> intercept(
            ConsumerRecord<String, String> record,
            Consumer<String, String> consumer
    ) {
        Context extracted = openTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.root(), record, OTEL_GETTER);
        SCOPE_HOLDER.set(extracted.makeCurrent());

        Header requestIdHeader = record.headers().lastHeader(REQUEST_ID);
        if (requestIdHeader != null && requestIdHeader.value() != null) {
            MDC.put(REQUEST_ID, new String(requestIdHeader.value(), StandardCharsets.UTF_8));
        }

        return record;
    }

    @Override
    public void clearThreadState(Consumer<?, ?> consumer) {
        Scope scope = SCOPE_HOLDER.get();
        if (scope != null) {
            scope.close();
            SCOPE_HOLDER.remove();
        }
        MDC.remove(REQUEST_ID);
    }
}
