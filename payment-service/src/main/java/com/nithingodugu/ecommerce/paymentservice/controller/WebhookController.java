package com.nithingodugu.ecommerce.paymentservice.controller;

import com.nithingodugu.ecommerce.paymentservice.dto.PaymentWebhookRequest;
import com.nithingodugu.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments/webhook")
@RequiredArgsConstructor
public class WebhookController {

    public final PaymentService paymentService;

    @PostMapping("")
    public ResponseEntity<Void> handleWebhook(@RequestBody PaymentWebhookRequest request){
        paymentService.handleWebhook(request);
        return ResponseEntity.accepted().build();
    }
}
