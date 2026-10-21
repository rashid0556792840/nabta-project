package com.nabta.backend.service;
import com.nabta.backend.dto.AdminDtos.*;
import com.nabta.backend.exception.ApiException;
import com.nabta.backend.model.*;
import com.nabta.backend.repository.CompanySettingsRepository;
import com.nabta.backend.repository.OrderRepository;
import com.nabta.backend.repository.ProductRepository;
import com.nabta.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
@Service
public class AdminService {
    private final OrderRepository orders; private final UserRepository users; private final ProductRepository products; private final CompanySettingsRepository settings;
    public AdminService(OrderRepository orders, UserRepository users, ProductRepository products, CompanySettingsRepository settings) { this.orders=orders; this.users=users; this.products=products; this.settings=settings; }
    public DashboardResponse dashboard() { return new DashboardResponse(orders.count(),orders.countByStatus(OrderStatus.PENDING),orders.countByStatus(OrderStatus.COMPLETED),orders.countByUser_Role(Role.CUSTOMER),products.countByActiveTrue(),products.countByStockQuantityLessThanAndActiveTrue(5),orders.sumTotalsByStatus(OrderStatus.COMPLETED)); }
    public java.util.List<AdminUserResponse> users() { return users.findAll().stream().map(u -> new AdminUserResponse(u.getId(),u.getName(),u.getEmail(),u.getPhone(),u.getAddress(),u.getRole(),u.getStatus())).toList(); }
    @Transactional public AdminUserResponse updateUserStatus(Long id, AccountStatus status, Long adminId) { AppUser u=users.findById(id).orElseThrow(() -> ApiException.notFound("User not found.")); if (u.getId().equals(adminId) && status==AccountStatus.DISABLED) throw ApiException.badRequest("You cannot disable your own administrator account."); u.setStatus(status); users.save(u); return new AdminUserResponse(u.getId(),u.getName(),u.getEmail(),u.getPhone(),u.getAddress(),u.getRole(),u.getStatus()); }
    @Transactional public AdminUserResponse updateUserRole(Long id, Role targetRole, Long adminId) {
        AppUser u=users.findById(id).orElseThrow(() -> ApiException.notFound("User not found."));
        if (u.getId().equals(adminId)) throw ApiException.badRequest("You cannot change your own role.");
        if (u.getRole()==Role.ADMIN) throw ApiException.badRequest("The Owner role cannot be changed through user role management.");
        if (targetRole==Role.ADMIN) throw ApiException.badRequest("Only the configured Owner account may have the Owner role.");
        if (targetRole!=Role.CUSTOMER && targetRole!=Role.SUPERVISOR && targetRole!=Role.DELIVERY) throw ApiException.badRequest("Unsupported account role.");
        u.setRole(targetRole); users.save(u);
        return new AdminUserResponse(u.getId(),u.getName(),u.getEmail(),u.getPhone(),u.getAddress(),u.getRole(),u.getStatus());
    }
    public CompanyResponse company() { CompanySettings s=settings.findById(1L).orElseGet(CompanySettings::new); return dto(s); }
    @Transactional public CompanyResponse updateCompany(CompanyRequest r) {
        CompanySettings s=settings.findById(1L).orElseGet(CompanySettings::new);
        if (r.companyName()!=null && !r.companyName().isBlank()) s.setCompanyName(r.companyName().trim());
        s.setPhone(r.phone()); s.setWhatsappNumber(r.whatsappNumber()); s.setEmail(r.email()); s.setAddress(r.address()); s.setLogoUrl(r.logoUrl());
        if (r.xUrl()!=null) s.setXUrl(r.xUrl().trim());
        if (r.instagramUrl()!=null) s.setInstagramUrl(r.instagramUrl().trim());
        if (r.snapchatUrl()!=null) s.setSnapchatUrl(r.snapchatUrl().trim());
        if (r.mapsUrl()!=null) s.setMapsUrl(r.mapsUrl().trim());
        return dto(settings.save(s));
    }
    private CompanyResponse dto(CompanySettings s) { return new CompanyResponse(s.getCompanyName(),s.getPhone(),s.getWhatsappNumber(),s.getEmail(),s.getAddress(),s.getLogoUrl(),s.getXUrl(),s.getInstagramUrl(),s.getSnapchatUrl(),s.getMapsUrl()); }
}
