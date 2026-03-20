package com.nithingodugu.ecommerce.paymentservice.outbox.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String aggregateType = "Product";

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    private String requestId;
    private String originalTraceId;
    private String originalSpanId;

    @CreationTimestamp
    private Instant createdAt;

    private Instant processedAt;

    private int retryCount = 0;
}
