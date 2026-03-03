package com.nithingodugu.ecommerce.paymentservice.service;

import com.nithingodugu.ecommerce.paymentservice.dto.PaymentCreateResponse;
import com.nithingodugu.ecommerce.paymentservice.dto.PaymentWebhookRequest;

public interface PaymentService {

//    PaymentServicePaymentResponse authorize(AuthorizePaymentRequest request);

    PaymentCreateResponse pay(String orderNumber);

    void handleWebhook(PaymentWebhookRequest request);

    PaymentCreateResponse getPayment(String paymentId);
}
