package com.nabta.backend.dto;
import com.nabta.backend.model.AccountStatus;
import com.nabta.backend.model.Role;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
public final class AdminDtos {
    private AdminDtos() {}
    public record DashboardResponse(long totalOrders, long pendingOrders, long completedOrders, long registeredCustomers, long activeProducts, long lowStockProducts, BigDecimal completedOrderValue) {}
    public record AdminUserResponse(Long id, String name, String email, String phone, String address, Role role, AccountStatus status) {}
    public record UserStatusRequest(@NotNull AccountStatus status) {}
    public record RoleUpdateRequest(@NotNull Role role) {}
    public record InventoryItem(Long productId, String productName, int stockQuantity, boolean active, boolean lowStock) {}
    public record CompanyRequest(String companyName, String phone, String whatsappNumber, String email, String address, String logoUrl,
                                 String xUrl, String instagramUrl, String snapchatUrl, String mapsUrl) {}
    public record CompanyResponse(String companyName, String phone, String whatsappNumber, String email, String address, String logoUrl,
                                  String xUrl, String instagramUrl, String snapchatUrl, String mapsUrl) {}
}
