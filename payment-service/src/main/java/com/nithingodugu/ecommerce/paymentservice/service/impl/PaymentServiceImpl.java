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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRespository paymentRespository;
    private final OrderClient orderClient;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentCreateResponse pay(String orderId){

        OrderDetailsResponse orderDetails = orderClient.getOrderDetails(orderId);

        if (!orderDetails.success()){
            throw new IllegalArgumentException("Invalid order id");
        }

        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID().toString());
        payment.setOrderId(orderId);
        payment.setAmount(orderDetails.amount());
        payment.setStatus(PaymentStatus.PENDING);

        payment = paymentRespository.save(payment);

        return new PaymentCreateResponse(
                orderId,
                payment.getPaymentId(),
                "/payments/webhook"

        );

    }

    @Override
    @Transactional
    public void handleWebhook(PaymentWebhookRequest request) {

        Payment payment = paymentRespository.findByPaymentId(request.paymentId()).orElseThrow();

        if (request.success()){
            payment.setStatus(PaymentStatus.AUTHORIZED);
            payment.setProviderReference(request.referenceId());
            paymentRespository.save(payment);

            PaymentAuthorizedEvent event = new PaymentAuthorizedEvent(payment.getOrderId());

            saveOutboxEvent(payment.getPaymentId(), KafkaTopics.PAYMENT_AUTHORIZED, event);

        }else{
            processPaymentFailed(payment, request.referenceId(), request.errorCode());
        }


    }

    @Override
    @Transactional
    public void processPaymentFailed(Payment payment, String reference, String errorCode){

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
            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to serialize outbox event for topic: " + topic, ex);
        }
    }

    @Override
    public PaymentCreateResponse getPayment(String paymentId) {

        Payment payment = paymentRespository
                .findByPaymentId(paymentId)
                .orElseThrow(()-> new PaymentNotFoundException("Payment not found"));

        return new PaymentCreateResponse(payment.getOrderId(), payment.getPaymentId(), "/");
    }

    @Override
    @Transactional
    public void processRefund(OrderCancelledEvent event) {

        Payment payment = paymentRespository
                .findByOrderId(event.getOrderId())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with OrderId=" + event.getOrderId()
                ));

        if (payment.getStatus() == PaymentStatus.REFUNDED){
            throw new DuplicateRefundException("Payment already refunded for order="+ event.getOrderId());
        }

        //DO REFUND
        payment.setStatus(PaymentStatus.REFUNDED);
    }



}
