package com.nabta.backend.dto;
import com.nabta.backend.model.OrderStatus;
import com.nabta.backend.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public final class OrderDtos {
    private OrderDtos() {}
    public record OrderItemRequest(@NotNull Long productId, @NotNull @Min(1) Integer quantity) {}
    public record CreateOrderRequest(@NotEmpty List<@Valid OrderItemRequest> items, @NotBlank @Size(max=1500) String deliveryAddress, @NotBlank @Pattern(regexp="^[0-9]{7,20}$", message="Phone must contain at least 7 digits and digits only.") @Size(max=20) String contactPhone, @Size(max=1500) String notes, PaymentMethod paymentMethod) {}
    public record OrderItemResponse(Long orderItemId, Long productId, String productName, String productNameAr, String imageUrl, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {}
    public record OrderResponse(Long id, String orderNumber, OrderStatus status, BigDecimal subtotal, BigDecimal deliveryCharge, BigDecimal totalAmount, String deliveryAddress, String contactPhone, String notes, PaymentMethod paymentMethod, List<OrderItemResponse> items, LocalDateTime createdAt, Long customerId, String customerName, String customerEmail) {}
    public record StatusUpdateRequest(@NotNull OrderStatus status) {}
}
