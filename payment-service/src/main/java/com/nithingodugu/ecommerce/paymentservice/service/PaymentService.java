package com.nithingodugu.ecommerce.paymentservice.service;

import com.nithingodugu.ecommerce.common.event.OrderCancelledEvent;
import com.nithingodugu.ecommerce.paymentservice.domain.entity.Payment;
import com.nithingodugu.ecommerce.paymentservice.dto.PaymentCreateResponse;
import com.nithingodugu.ecommerce.paymentservice.dto.PaymentWebhookRequest;

public interface PaymentService {

//    PaymentServicePaymentResponse authorize(AuthorizePaymentRequest request);

    PaymentCreateResponse pay(String orderNumber);

    void handleWebhook(PaymentWebhookRequest request);

    PaymentCreateResponse getPayment(String paymentId);

    void processRefund(OrderCancelledEvent event);

    void processPaymentFailed(Payment payment, String reference, String errorCode);
}
