package com.nabta.backend.model;

/**
 * Payment methods recognized by Nabta. Cash on delivery is active while no provider is connected.
 * Online methods are planned capabilities and must not be treated as enabled until a real provider
 * verifies payment server-side.
 */
public enum PaymentMethod {
    CASH_ON_DELIVERY,
    MADA,
    VISA,
    MASTERCARD,
    APPLE_PAY,
    STC_PAY,
    SAMSUNG_PAY,
    OTHER_ONLINE
}
