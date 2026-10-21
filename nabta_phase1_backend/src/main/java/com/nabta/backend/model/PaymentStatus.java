package com.nabta.backend.model;

/** Lifecycle statuses for real provider transactions. This phase creates the model only. */
public enum PaymentStatus {
    CREATED,
    PENDING,
    REQUIRES_ACTION,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    REFUNDED,
    EXPIRED
}
