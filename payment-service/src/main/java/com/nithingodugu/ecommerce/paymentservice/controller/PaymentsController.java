package com.nithingodugu.ecommerce.paymentservice.controller;

import com.nithingodugu.ecommerce.paymentservice.dto.PaymentCreateResponse;
import com.nithingodugu.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/payments")
public class PaymentsController {

    private final PaymentService paymentService;

    @PostMapping("/pay/{orderNumber}")
    public PaymentCreateResponse pay(
            @PathVariable String orderNumber
    ){
        return paymentService.pay(orderNumber);
    }

    @GetMapping("/{paymentId}")
    private PaymentCreateResponse getPayment(@PathVariable String paymentId){
        return paymentService.getPayment(paymentId);
    }

}
