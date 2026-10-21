package com.nabta.backend.event;

public record OrderCreatedEvent(Long orderId, String orderNumber) {}
