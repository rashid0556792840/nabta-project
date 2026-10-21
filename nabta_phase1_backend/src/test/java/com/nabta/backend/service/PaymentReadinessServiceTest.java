package com.nabta.backend.service;

import com.nabta.backend.payment.PaymentGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentReadinessServiceTest {
    @Test
    void noGatewayKeepsCheckoutDisabledAndReportsSaudiDefaults() {
        @SuppressWarnings("unchecked")
        ObjectProvider<PaymentGateway> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        PaymentReadinessService service = new PaymentReadinessService(
            provider, "SA", "SAR", "none", false, false
        );

        var result = service.getReadiness();
        assertEquals("SA", result.countryCode());
        assertEquals("SAR", result.currency());
        assertTrue(result.onlinePaymentsOnly());
        assertFalse(result.cashOnDeliveryEnabled());
        assertFalse(result.providerAdapterRegistered());
        assertFalse(result.providerEnabled());
        assertFalse(result.checkoutEnabled());
        assertEquals("PROVIDER_NOT_CONNECTED", result.statusCode());
        assertTrue(result.enabledMethods().isEmpty());
        assertTrue(result.plannedMethods().contains("MADA"));
        assertTrue(result.plannedMethods().contains("VISA"));
    }
}
