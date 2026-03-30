package com.nithingodugu.ecommerce.paymentservice.controller;

import com.nithingodugu.ecommerce.paymentservice.dto.PaymentCreateResponse;
import com.nithingodugu.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/payments")
@PreAuthorize("isAuthenticated()")
public class PaymentsController {

    private final PaymentService paymentService;

    @PostMapping("/pay/{orderId}")
    public PaymentCreateResponse pay(
            @AuthenticationPrincipal String userId,
            @PathVariable String orderId
    ){
        return paymentService.pay(userId, orderId);
    }

    @GetMapping("/{paymentId}")
    private PaymentCreateResponse getPayment(@PathVariable String paymentId){
        return paymentService.getPayment(paymentId);
    }

}
