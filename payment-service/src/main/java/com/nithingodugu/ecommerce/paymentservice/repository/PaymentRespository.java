package com.nithingodugu.ecommerce.paymentservice.repository;

import com.nithingodugu.ecommerce.paymentservice.domain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRespository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderNumber(String orderNumber);

    Optional<Payment> findByPaymentId(String paymentId);
}
