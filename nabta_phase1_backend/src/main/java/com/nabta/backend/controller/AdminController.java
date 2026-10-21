package com.nabta.backend.controller;
import com.nabta.backend.dto.AdminDtos.*;
import com.nabta.backend.dto.CatalogDtos.*;
import com.nabta.backend.dto.OrderDtos.*;
import com.nabta.backend.dto.OrderWorkflowDtos.*;
import com.nabta.backend.model.AccountStatus;
import com.nabta.backend.model.AppUser;
import com.nabta.backend.model.OrderStatus;
import com.nabta.backend.model.DeliveryAssignmentStatus;
import com.nabta.backend.model.Role;
import com.nabta.backend.service.AdminService;
import com.nabta.backend.service.CatalogService;
import com.nabta.backend.service.OrderService;
import com.nabta.backend.service.OrderWorkflowService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/admin")
public class AdminController {
    private final AdminService admin; private final CatalogService catalog; private final OrderService orders; private final OrderWorkflowService workflow;
    public AdminController(AdminService admin,CatalogService catalog,OrderService orders,OrderWorkflowService workflow) { this.admin=admin; this.catalog=catalog; this.orders=orders; this.workflow=workflow; }
    @GetMapping("/dashboard") public DashboardResponse dashboard() { return admin.dashboard(); }
    @GetMapping("/products") public Page<ProductResponse> products(@RequestParam(required=false) String q,@RequestParam(required=false) Long categoryId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return catalog.products(q,categoryId,true,PageRequest.of(Math.max(0,page),Math.max(1,Math.min(size,100)),Sort.by("createdAt").descending())); }
    @PostMapping("/products") public ProductResponse createProduct(@Valid @RequestBody ProductRequest request) { return catalog.createProduct(request); }
    @PutMapping("/products/{id}") public ProductResponse updateProduct(@PathVariable Long id,@Valid @RequestBody ProductRequest request) { return catalog.updateProduct(id,request); }
    @DeleteMapping("/products/{id}") public void deleteProduct(@PathVariable Long id) { catalog.deactivateProduct(id); }
    @GetMapping("/categories") public List<?> categories() { return catalog.categories(true); }
    @PostMapping("/categories") public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) { return catalog.createCategory(request); }
    @PutMapping("/categories/{id}") public CategoryResponse updateCategory(@PathVariable Long id,@Valid @RequestBody CategoryRequest request) { return catalog.updateCategory(id,request); }
    @DeleteMapping("/categories/{id}") public void deleteCategory(@PathVariable Long id) { catalog.deactivateCategory(id); }
    @GetMapping("/inventory") public List<CatalogService.InventoryItem> inventory() { return catalog.inventory(5); }
    @PutMapping("/inventory/{productId}") public ProductResponse stock(@PathVariable Long productId,@RequestParam int quantity) { return catalog.changeStock(productId,quantity); }
    @GetMapping("/orders") public Page<OrderResponse> orders(@RequestParam(required=false) OrderStatus status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return this.orders.adminOrders(status,PageRequest.of(Math.max(0,page),Math.max(1,Math.min(size,100)),Sort.by("createdAt").descending())); }
    @PutMapping("/orders/{id}/status") public OrderResponse updateOrderStatus(@AuthenticationPrincipal AppUser current,@PathVariable Long id,@Valid @RequestBody StatusUpdateRequest request) { workflow.updateStatusByOwner(id,request.status(),current); return orders.operationsOrder(id); }
    @PostMapping("/orders/{id}/dispatch") public DeliveryAssignmentResponse dispatchOrder(@AuthenticationPrincipal AppUser current,@PathVariable Long id,@Valid @RequestBody DispatchRequest request) { return workflow.createDispatch(id,request,current); }
    @GetMapping("/assignments") public org.springframework.data.domain.Page<DeliveryAssignmentResponse> assignments(@AuthenticationPrincipal AppUser current,@RequestParam(required=false) DeliveryAssignmentStatus status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return workflow.operationsAssignments(current,status,PageRequest.of(Math.max(0,page),Math.max(1,Math.min(size,100)),Sort.by("createdAt").descending())); }
    @PostMapping("/assignments/{id}/cancel") public DeliveryAssignmentResponse cancelAssignment(@AuthenticationPrincipal AppUser current,@PathVariable Long id) { return workflow.cancelDispatch(id,current); }
    @GetMapping("/orders/{id}/history") public List<OrderStatusHistoryResponse> orderHistory(@AuthenticationPrincipal AppUser current,@PathVariable Long id) { return workflow.operationsHistory(id,current); }
    @GetMapping("/users") public List<AdminUserResponse> users() { return admin.users(); }
    @PutMapping("/users/{id}/status") public AdminUserResponse userStatus(@AuthenticationPrincipal AppUser current,@PathVariable Long id,@Valid @RequestBody UserStatusRequest request) { return admin.updateUserStatus(id,request.status(),current.getId()); }
    @PutMapping("/users/{id}/role") public AdminUserResponse userRole(@AuthenticationPrincipal AppUser current,@PathVariable Long id,@Valid @RequestBody RoleUpdateRequest request) { return admin.updateUserRole(id,request.role(),current.getId()); }
    @PutMapping("/company") public CompanyResponse updateCompany(@RequestBody CompanyRequest request) { return admin.updateCompany(request); }
}
