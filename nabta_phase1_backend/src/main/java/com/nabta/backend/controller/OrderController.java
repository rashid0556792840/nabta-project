package com.nabta.backend.controller;
import com.nabta.backend.dto.OrderDtos.*;
import com.nabta.backend.dto.OrderWorkflowDtos.*;
import com.nabta.backend.model.AppUser;
import com.nabta.backend.model.OrderStatus;
import com.nabta.backend.service.OrderService;
import com.nabta.backend.service.OrderWorkflowService;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orders; private final OrderWorkflowService workflow; public OrderController(OrderService orders, OrderWorkflowService workflow) { this.orders=orders; this.workflow=workflow; }
    @PostMapping public OrderResponse create(@AuthenticationPrincipal AppUser user,@RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody CreateOrderRequest request) { return orders.create(user,request,key); }
    @GetMapping("/my") public Page<OrderResponse> myOrders(@AuthenticationPrincipal AppUser user,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return orders.myOrders(user.getId(),PageRequest.of(Math.max(0,page),Math.max(1,Math.min(size,100)),Sort.by("createdAt").descending())); }
    @GetMapping("/{id}") public OrderResponse myOrder(@AuthenticationPrincipal AppUser user,@PathVariable Long id) { return orders.myOrder(id,user.getId()); }
    @GetMapping("/{id}/history") public List<OrderStatusHistoryResponse> history(@AuthenticationPrincipal AppUser user,@PathVariable Long id) { return workflow.customerHistory(id,user.getId()); }
    @GetMapping("/{id}/deliveries") public List<DeliveryAssignmentResponse> deliveries(@AuthenticationPrincipal AppUser user,@PathVariable Long id) { return workflow.customerDeliveries(id,user.getId()); }
}
