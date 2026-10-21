package com.nabta.backend.controller;

import com.nabta.backend.dto.AdminDtos.AdminUserResponse;
import com.nabta.backend.dto.OrderDtos.OrderResponse;
import com.nabta.backend.dto.OrderDtos.StatusUpdateRequest;
import com.nabta.backend.dto.OrderWorkflowDtos.*;
import com.nabta.backend.model.AppUser;
import com.nabta.backend.model.DeliveryAssignmentStatus;
import com.nabta.backend.model.OrderStatus;
import com.nabta.backend.model.Role;
import com.nabta.backend.repository.UserRepository;
import com.nabta.backend.service.OrderService;
import com.nabta.backend.service.OrderWorkflowService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/supervisor")
public class SupervisorController {
    private final OrderService orders;
    private final OrderWorkflowService workflow;
    private final UserRepository users;
    public SupervisorController(OrderService orders, OrderWorkflowService workflow, UserRepository users) { this.orders = orders; this.workflow = workflow; this.users=users; }

    @GetMapping("/delivery-users")
    public List<AdminUserResponse> deliveryUsers() { return users.findAll().stream().filter(u -> u.getRole()==Role.DELIVERY && u.isEnabled()).map(u -> new AdminUserResponse(u.getId(),u.getName(),u.getEmail(),u.getPhone(),u.getAddress(),u.getRole(),u.getStatus())).toList(); }

    @GetMapping("/orders")
    public Page<OrderResponse> orders(@RequestParam(required=false) OrderStatus status, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return orders.adminOrders(status, PageRequest.of(Math.max(0,page), Math.max(1,Math.min(size,100)), Sort.by("createdAt").descending()));
    }

    @PutMapping("/orders/{id}/status")
    public OrderResponse updateStatus(@AuthenticationPrincipal AppUser actor, @PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        workflow.updateStatusBySupervisor(id, request.status(), actor);
        return orders.operationsOrder(id);
    }

    @PostMapping("/orders/{id}/dispatch")
    public DeliveryAssignmentResponse dispatch(@AuthenticationPrincipal AppUser actor, @PathVariable Long id, @Valid @RequestBody DispatchRequest request) {
        return workflow.createDispatch(id, request, actor);
    }

    @GetMapping("/assignments")
    public Page<DeliveryAssignmentResponse> assignments(@AuthenticationPrincipal AppUser actor, @RequestParam(required=false) DeliveryAssignmentStatus status, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return workflow.operationsAssignments(actor, status, PageRequest.of(Math.max(0,page), Math.max(1,Math.min(size,100)), Sort.by("createdAt").descending()));
    }

    @PostMapping("/assignments/{id}/cancel")
    public DeliveryAssignmentResponse cancelAssignment(@AuthenticationPrincipal AppUser actor, @PathVariable Long id) { return workflow.cancelDispatch(id, actor); }

    @GetMapping("/orders/{id}/history")
    public List<OrderStatusHistoryResponse> history(@AuthenticationPrincipal AppUser actor, @PathVariable Long id) { return workflow.operationsHistory(id, actor); }
}
