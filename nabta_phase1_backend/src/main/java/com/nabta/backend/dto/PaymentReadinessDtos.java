package com.nabta.backend.dto;

import java.util.List;

public final class PaymentReadinessDtos {
    private PaymentReadinessDtos() {}

    public record PaymentReadinessResponse(
        String countryCode,
        String currency,
        boolean onlinePaymentsOnly,
        boolean cashOnDeliveryEnabled,
        String configuredProvider,
        boolean providerAdapterRegistered,
        boolean providerEnabled,
        boolean checkoutEnabled,
        String statusCode,
        List<String> enabledMethods,
        List<String> plannedMethods
    ) {}
}
