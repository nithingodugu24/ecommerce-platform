package com.nithingodugu.ecommerce.paymentservice.scheduler;

import com.nithingodugu.ecommerce.paymentservice.domain.entity.Payment;
import com.nithingodugu.ecommerce.paymentservice.domain.enums.PaymentStatus;
import com.nithingodugu.ecommerce.paymentservice.repository.PaymentRespository;
import com.nithingodugu.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentExpiryScheduler {

    private final PaymentRespository paymentRespository;
    private final PaymentService paymentService;

    @Scheduled(fixedDelay = 60_000)
    public void paymentTimeout(){

        Instant cotOff = Instant.now().minus(15, ChronoUnit.MINUTES);

        List<Payment> stalePayments = paymentRespository
                .findByStatusAndCreatedAtBefore(PaymentStatus.PENDING, cotOff);

        for (Payment payment: stalePayments){
            paymentService.processPaymentFailed(
                    payment,
                    "",
                    "TIMEOUT"
            );
        }


    }
}
