package com.nabta.backend.event;

import com.nabta.backend.model.Role;

public record OrderWorkflowEvent(
    String type,
    Long orderId,
    String orderNumber,
    Long assignmentId,
    Long recipientUserId,
    Role recipientRole,
    String title,
    String body,
    String status
) {}
