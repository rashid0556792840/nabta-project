package com.nabta.backend.service;

import com.nabta.backend.dto.PaymentReadinessDtos.PaymentReadinessResponse;
import com.nabta.backend.payment.PaymentGateway;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;

/** Reports readiness without exposing credentials or pretending a provider has been connected. */
@Service
public class PaymentReadinessService {
    private static final List<String> PLANNED_METHODS = List.of(
        "MADA", "VISA", "MASTERCARD", "APPLE_PAY", "STC_PAY", "SAMSUNG_PAY"
    );

    private final ObjectProvider<PaymentGateway> gatewayProvider;
    private final String countryCode;
    private final String currency;
    private final String configuredProvider;
    private final boolean providerEnabled;
    private final boolean checkoutFlowImplemented;

    public PaymentReadinessService(
        ObjectProvider<PaymentGateway> gatewayProvider,
        @Value("${nabta.payment.country:SA}") String countryCode,
        @Value("${nabta.payment.currency:SAR}") String currency,
        @Value("${nabta.payment.provider:none}") String configuredProvider,
        @Value("${nabta.payment.enabled:false}") boolean providerEnabled,
        @Value("${nabta.payment.checkout-flow-implemented:false}") boolean checkoutFlowImplemented
    ) {
        this.gatewayProvider = gatewayProvider;
        this.countryCode = countryCode;
        this.currency = currency;
        this.configuredProvider = configuredProvider;
        this.providerEnabled = providerEnabled;
        this.checkoutFlowImplemented = checkoutFlowImplemented;
    }

    public PaymentReadinessResponse getReadiness() {
        PaymentGateway gateway = gatewayProvider.getIfAvailable();
        boolean adapterRegistered = gateway != null
            && configuredProvider != null
            && !configuredProvider.isBlank()
            && !"none".equalsIgnoreCase(configuredProvider)
            && configuredProvider.equalsIgnoreCase(gateway.providerCode());
        boolean enabled = adapterRegistered && providerEnabled;
        // This flag remains false in this phase. A provider adapter alone must never activate orders.
        boolean checkoutEnabled = enabled && checkoutFlowImplemented;
        String status = checkoutEnabled ? "READY" : !adapterRegistered ? "PROVIDER_NOT_CONNECTED"
            : !enabled ? "PAYMENTS_DISABLED" : "CHECKOUT_FLOW_NOT_ENABLED";
        return new PaymentReadinessResponse(
            countryCode, currency, true, false,
            configuredProvider == null || configuredProvider.isBlank() ? "none" : configuredProvider,
            adapterRegistered, enabled, checkoutEnabled, status,
            checkoutEnabled ? PLANNED_METHODS : List.of(), PLANNED_METHODS
        );
    }

    public boolean isCheckoutEnabled() {
        return getReadiness().checkoutEnabled();
    }
}
