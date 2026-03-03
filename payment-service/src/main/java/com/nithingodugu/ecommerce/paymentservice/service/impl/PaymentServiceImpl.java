package com.nithingodugu.ecommerce.paymentservice.service.impl;

import com.nithingodugu.ecommerce.common.contract.order.OrderDetailsResponse;
import com.nithingodugu.ecommerce.common.event.PaymentAuthorizedEvent;
import com.nithingodugu.ecommerce.common.event.PaymentFailedEvent;
import com.nithingodugu.ecommerce.paymentservice.client.OrderClient;
import com.nithingodugu.ecommerce.paymentservice.config.KafkaConfig;
import com.nithingodugu.ecommerce.paymentservice.domain.entity.Payment;
import com.nithingodugu.ecommerce.paymentservice.domain.enums.PaymentStatus;
import com.nithingodugu.ecommerce.paymentservice.dto.PaymentCreateResponse;
import com.nithingodugu.ecommerce.paymentservice.dto.PaymentWebhookRequest;
import com.nithingodugu.ecommerce.paymentservice.repository.PaymentRespository;
import com.nithingodugu.ecommerce.paymentservice.service.PaymentService;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentCreateResponse pay(String orderNumber){

        OrderDetailsResponse orderDetails = orderClient.getOrderDetails(orderNumber);
        if (!orderDetails.success()){
            throw new IllegalArgumentException("Invalid order id");
        }

        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID().toString());
        payment.setOrderNumber(orderNumber);
        payment.setAmount(orderDetails.amount());
        payment.setStatus(PaymentStatus.PENDING);

        payment = paymentRespository.save(payment);

        return new PaymentCreateResponse(
                orderNumber,
                payment.getPaymentId(),
                "/payments/webhook"

        );

    }

    @Override
    public void handleWebhook(PaymentWebhookRequest request) {

        Payment payment = paymentRespository.findByPaymentId(request.paymentId()).orElseThrow();

        if (request.success()){
            payment.setStatus(PaymentStatus.AUTHORIZED);
            payment.setProviderReference(request.referenceId());
            paymentRespository.save(payment);

            PaymentAuthorizedEvent event = new PaymentAuthorizedEvent(payment.getOrderNumber());
            kafkaTemplate.send("payment.authorized", event);

        }else{
            payment.setStatus(PaymentStatus.FAILED);
            payment.setProviderReference(request.referenceId());
            payment.setErrorCode(request.errorCode());
            paymentRespository.save(payment);

            PaymentFailedEvent event = new PaymentFailedEvent(payment.getOrderNumber(), payment.getErrorCode());
            kafkaTemplate.send("payment.failed", event);

        }


    }

    @Override
    public PaymentCreateResponse getPayment(String paymentId) {
        Payment payment = paymentRespository.findByPaymentId(paymentId).orElseThrow();

        return new PaymentCreateResponse(payment.getOrderNumber(), payment.getPaymentId(), "/");
    }


//    @Override
//    public PaymentResponse authorize(AuthorizePaymentRequest request) {
//
//        Payment payment = paymentRespository.findByOrderId(request.orderId()).orElseGet(Payment::new);
//        payment.setOrderId(Long.valueOf(request.orderId()));
//        payment.setAmount(request.amount());
//        payment.setPaymentToken(request.paymentToken());
//
//        if (request.paymentToken().equals("pm_test_declined")){
//
//            payment.setStatus(PaymentStatus.FAILED);
//            payment.setErrorCode("CARD_DECLINED");
//            payment = paymentRespository.save(payment);
//
//            return new PaymentResponse(
//                    false,
//                    payment.getId().toString(),
//                    payment.getStatus().name(),
//                    payment.getErrorCode(),
//                    "Mock decline for test token"
//            );
//        }
//
//        payment.setStatus(PaymentStatus.AUTHORIZED);
//        payment.setProviderReference("mock-auth-" + payment.getId());
//        payment = paymentRespository.save(payment);
//
//        return new PaymentResponse(
//                true,
//                payment.getId().toString(),
//                payment.getStatus().name(),
//                null,
//                "Payment Authorized"
//        );
//
//
//    }

}
