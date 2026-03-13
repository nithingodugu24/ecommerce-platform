package com.nithingodugu.ecommerce.paymentservice.controller;

import com.nithingodugu.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/payments")
@RequiredArgsConstructor
public class PaymentsInternalController {

    private final PaymentService paymentService;


}
