package com.nabta.backend.dto;

import com.nabta.backend.model.DeliveryAssignmentStatus;
import com.nabta.backend.model.OrderStatus;
import com.nabta.backend.model.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderWorkflowDtos {
    private OrderWorkflowDtos() {}
    public record DispatchItemRequest(@NotNull Long orderItemId, @NotNull @Min(1) Integer quantity) {}
    public record DispatchRequest(@NotNull Long deliveryUserId, @NotNull Boolean sendAllPendingItems, @Size(max=1000) String note, List<@Valid DispatchItemRequest> items) {}
    public record DeliveryAssignmentItemResponse(Long orderItemId, Long productId, String productName, String productNameAr, String imageUrl, int orderedQuantity, int assignedQuantity, BigDecimal unitPrice) {}
    public record DeliveryAssignmentResponse(Long id, Long orderId, String orderNumber, Long deliveryUserId, String deliveryUserName, String assignedByName, DeliveryAssignmentStatus status, List<DeliveryAssignmentItemResponse> items, String note, LocalDateTime assignedAt, LocalDateTime acceptedAt, LocalDateTime deliveredAt) {}
    public record OrderStatusHistoryResponse(Long id, OrderStatus previousStatus, OrderStatus newStatus, String note, String actorName, Role actorRole, LocalDateTime createdAt) {}
    public record SupervisorOrderResponse(Long id, String orderNumber, OrderStatus status, BigDecimal totalAmount, String customerName, String customerEmail, String contactPhone, String deliveryAddress, LocalDateTime createdAt) {}
}
