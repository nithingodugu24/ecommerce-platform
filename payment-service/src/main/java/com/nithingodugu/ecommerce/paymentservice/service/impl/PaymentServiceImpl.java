package com.nithingodugu.ecommerce.paymentservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nithingodugu.ecommerce.common.contract.order.OrderDetailsResponse;
import com.nithingodugu.ecommerce.common.event.OrderCancelledEvent;
import com.nithingodugu.ecommerce.common.event.PaymentAuthorizedEvent;
import com.nithingodugu.ecommerce.common.event.PaymentFailedEvent;
import com.nithingodugu.ecommerce.paymentservice.client.OrderClient;
import com.nithingodugu.ecommerce.paymentservice.config.KafkaConfig;
import com.nithingodugu.ecommerce.paymentservice.domain.entity.Payment;
import com.nithingodugu.ecommerce.paymentservice.domain.enums.PaymentStatus;
import com.nithingodugu.ecommerce.paymentservice.dto.PaymentCreateResponse;
import com.nithingodugu.ecommerce.paymentservice.dto.PaymentWebhookRequest;
import com.nithingodugu.ecommerce.paymentservice.exceptions.DuplicateRefundException;
import com.nithingodugu.ecommerce.paymentservice.exceptions.PaymentNotFoundException;
import com.nithingodugu.ecommerce.paymentservice.kafka.KafkaTopics;
import com.nithingodugu.ecommerce.paymentservice.outbox.entity.OutboxEvent;
import com.nithingodugu.ecommerce.paymentservice.outbox.entity.OutboxStatus;
import com.nithingodugu.ecommerce.paymentservice.outbox.repository.OutboxEventRepository;
import com.nithingodugu.ecommerce.paymentservice.repository.PaymentRespository;
import com.nithingodugu.ecommerce.paymentservice.service.PaymentService;
import io.opentelemetry.api.trace.Span;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRespository paymentRespository;
    private final OrderClient orderClient;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentCreateResponse pay(String orderId){

        log.info("Payment request initiated",
                kv("orderId", orderId)
        );

        OrderDetailsResponse orderDetails = orderClient.getOrderDetails(orderId);

        if (!orderDetails.success()){
            log.warn("Payment failed - invalid order",
                    kv("orderId", orderId),
                    kv("reason", orderDetails.message())
            );
            throw new IllegalArgumentException("Invalid order id");
        }

        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID().toString());
        payment.setOrderId(orderId);
        payment.setAmount(orderDetails.amount());
        payment.setStatus(PaymentStatus.PENDING);

        payment = paymentRespository.save(payment);

        log.info("Payment created",
                kv("paymentId", payment.getPaymentId()),
                kv("orderId", orderId),
                kv("amount", payment.getAmount())
        );

        return new PaymentCreateResponse(
                orderId,
                payment.getPaymentId(),
                "/payments/webhook"

        );

    }

    @Override
    @Transactional
    public void handleWebhook(PaymentWebhookRequest request) {

        log.info("Payment webhook received",
                kv("paymentId", request.paymentId()),
                kv("success", request.success())
        );

        Payment payment = paymentRespository.findByPaymentId(request.paymentId()).orElseThrow(() -> {
            log.warn("Webhook failed - payment not found",
                    kv("paymentId", request.paymentId()));
            return new PaymentNotFoundException("Payment not found");
        });

        if (request.success()){
            payment.setStatus(PaymentStatus.AUTHORIZED);
            payment.setProviderReference(request.referenceId());
            paymentRespository.save(payment);

            log.info("Payment authorized",
                    kv("paymentId", payment.getPaymentId()),
                    kv("orderId", payment.getOrderId())
            );

            PaymentAuthorizedEvent event = new PaymentAuthorizedEvent(payment.getOrderId());

            saveOutboxEvent(payment.getPaymentId(), KafkaTopics.PAYMENT_AUTHORIZED, event);

        }else{
            processPaymentFailed(payment, request.referenceId(), request.errorCode());
        }

        log.info("Payment webhook processed",
                kv("paymentId", payment.getPaymentId()),
                kv("orderId", payment.getOrderId())
        );


    }

    @Override
    @Transactional
    public void processPaymentFailed(Payment payment, String reference, String errorCode){

        log.warn("Payment failed",
                kv("paymentId", payment.getPaymentId()),
                kv("orderId", payment.getOrderId()),
                kv("errorCode", errorCode)
        );

        payment.setStatus(PaymentStatus.FAILED);
        payment.setProviderReference(reference);
        payment.setErrorCode(errorCode);
        paymentRespository.save(payment);

        PaymentFailedEvent event = new PaymentFailedEvent(payment.getOrderId(), payment.getErrorCode());

        saveOutboxEvent(payment.getPaymentId(), KafkaTopics.PAYMENT_FAILED, event);
    }

    private void saveOutboxEvent(String aggregateId, String topic, Object event) {
        try {
            OutboxEvent outbox = new OutboxEvent();
            outbox.setEventId(UUID.randomUUID().toString());
            outbox.setAggregateId(aggregateId);
            outbox.setTopic(topic);
            outbox.setPayload(objectMapper.writeValueAsString(event));
            outbox.setStatus(OutboxStatus.PENDING);

            outbox.setRequestId(MDC.get("requestId"));

            var currentSpan = Span.current().getSpanContext();
            if (currentSpan.isValid()) {
                outbox.setOriginalTraceId(currentSpan.getTraceId());
                outbox.setOriginalSpanId(currentSpan.getSpanId());
            }

            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize " + event.getClass().getName() + " event", kv("error", ex.getMessage()));
            throw new RuntimeException("Failed to serialize outbox event for topic: " + topic, ex);
        }
    }

    @Override
    public PaymentCreateResponse getPayment(String paymentId) {

        log.debug("Get payment request",
                kv("paymentId", paymentId)
        );

        Payment payment = paymentRespository
                .findByPaymentId(paymentId)
                .orElseThrow(() -> {
                    log.warn("Get payment failed",
                            kv("paymentId", paymentId));
                    return new PaymentNotFoundException("Payment not found");
                });

        return new PaymentCreateResponse(payment.getOrderId(), payment.getPaymentId(), "/");
    }

    @Override
    @Transactional
    public void processRefund(OrderCancelledEvent event) {

        log.info("Refund initiated",
                kv("orderId", event.getOrderId())
        );


        Payment payment = paymentRespository
                .findByOrderId(event.getOrderId())
                .orElseThrow(() -> {
                    log.warn("Refund failed - payment not found",
                            kv("orderId", event.getOrderId()));
                    return new PaymentNotFoundException(
                            "Payment not found with OrderId=" + event.getOrderId()
                    );
                });

        if (payment.getStatus() == PaymentStatus.REFUNDED){
            log.warn("Duplicate refund attempt",
                    kv("orderId", event.getOrderId()));
            throw new DuplicateRefundException("Payment already refunded for order="+ event.getOrderId());
        }

        //DO REFUND
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRespository.save(payment);

        log.info("Refund completed",
                kv("paymentId", payment.getPaymentId()),
                kv("orderId", event.getOrderId())
        );

    }



}
