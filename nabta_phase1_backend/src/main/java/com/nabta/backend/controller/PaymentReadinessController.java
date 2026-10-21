package com.nabta.backend.controller;

import com.nabta.backend.dto.PaymentReadinessDtos.PaymentReadinessResponse;
import com.nabta.backend.service.PaymentReadinessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public, non-sensitive readiness information for the checkout interface. */
@RestController
@RequestMapping("/api/payments")
public class PaymentReadinessController {
    private final PaymentReadinessService readiness;

    public PaymentReadinessController(PaymentReadinessService readiness) {
        this.readiness = readiness;
    }

    @GetMapping("/readiness")
    public PaymentReadinessResponse readiness() {
        return readiness.getReadiness();
    }
}
