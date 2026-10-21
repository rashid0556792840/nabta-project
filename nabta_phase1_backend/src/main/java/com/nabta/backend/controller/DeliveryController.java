package com.nabta.backend.controller;

import com.nabta.backend.dto.OrderWorkflowDtos.DeliveryAssignmentResponse;
import com.nabta.backend.model.AppUser;
import com.nabta.backend.model.DeliveryAssignmentStatus;
import com.nabta.backend.service.OrderWorkflowService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryController {
    private final OrderWorkflowService workflow;
    public DeliveryController(OrderWorkflowService workflow) { this.workflow = workflow; }

    @GetMapping("/assignments")
    public Page<DeliveryAssignmentResponse> assignments(@AuthenticationPrincipal AppUser actor, @RequestParam(required=false) DeliveryAssignmentStatus status, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return workflow.deliveryAssignments(actor, status, PageRequest.of(Math.max(0,page), Math.max(1,Math.min(size,100)), Sort.by("createdAt").descending()));
    }

    @GetMapping("/assignments/{id}")
    public DeliveryAssignmentResponse assignment(@AuthenticationPrincipal AppUser actor, @PathVariable Long id) { return workflow.deliveryAssignment(id, actor); }

    @PostMapping("/assignments/{id}/accept")
    public DeliveryAssignmentResponse accept(@AuthenticationPrincipal AppUser actor, @PathVariable Long id) { return workflow.acceptDispatch(id, actor); }

    @PostMapping("/assignments/{id}/complete")
    public DeliveryAssignmentResponse complete(@AuthenticationPrincipal AppUser actor, @PathVariable Long id) { return workflow.completeDispatch(id, actor); }
}
