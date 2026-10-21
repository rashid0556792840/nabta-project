package com.nabta.backend.payment;

import com.nabta.backend.model.PaymentMethod;
import java.math.BigDecimal;
import java.net.URI;

/**
 * Provider-neutral contract for a future payment adapter. No implementation is shipped in this
 * phase. A provider adapter must create a real payment session and verify it server-side before
 * Nabta may mark a transaction paid or allow an order into processing.
 */
public interface PaymentGateway {
    String providerCode();

    PaymentSession createPaymentSession(PaymentRequest request);

    PaymentVerification verifyPayment(String providerPaymentId);

    record PaymentRequest(
        String merchantReference,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        String customerEmail,
        String customerPhone,
        URI returnUrl,
        URI cancelUrl
    ) {}

    record PaymentSession(
        String providerPaymentId,
        URI checkoutUrl,
        String providerStatus
    ) {}

    record PaymentVerification(
        boolean successful,
        String providerPaymentId,
        BigDecimal amount,
        String currency,
        String merchantReference,
        String providerStatus
    ) {}
}
