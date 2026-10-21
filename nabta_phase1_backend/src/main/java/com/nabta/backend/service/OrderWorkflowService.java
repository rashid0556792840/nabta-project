package com.nabta.backend.service;

import com.nabta.backend.dto.OrderWorkflowDtos.*;
import com.nabta.backend.event.OrderWorkflowEvent;
import com.nabta.backend.exception.ApiException;
import com.nabta.backend.model.*;
import com.nabta.backend.repository.DeliveryAssignmentRepository;
import com.nabta.backend.repository.OrderRepository;
import com.nabta.backend.repository.OrderStatusHistoryRepository;
import com.nabta.backend.repository.ProductRepository;
import com.nabta.backend.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderWorkflowService {
    private static final List<DeliveryAssignmentStatus> ACTIVE_ASSIGNMENT_STATUSES = List.of(DeliveryAssignmentStatus.ASSIGNED, DeliveryAssignmentStatus.ACCEPTED);
    private final OrderRepository orders;
    private final UserRepository users;
    private final ProductRepository products;
    private final DeliveryAssignmentRepository assignments;
    private final OrderStatusHistoryRepository history;
    private final ApplicationEventPublisher events;

    public OrderWorkflowService(OrderRepository orders, UserRepository users, ProductRepository products,
                                DeliveryAssignmentRepository assignments, OrderStatusHistoryRepository history,
                                ApplicationEventPublisher events) {
        this.orders = orders;
        this.users = users;
        this.products = products;
        this.assignments = assignments;
        this.history = history;
        this.events = events;
    }

    @Transactional public CustomerOrder updateStatusByOwner(Long orderId, OrderStatus target, AppUser actor) {
        requireRole(actor, Role.ADMIN);
        if (target == OrderStatus.COMPLETED || target == OrderStatus.OUT_FOR_DELIVERY || target == OrderStatus.PARTIALLY_DELIVERED) {
            throw ApiException.badRequest("Only the assigned delivery person can complete a delivery. Out-for-delivery status is set when the delivery person accepts an assignment.");
        }
        return applySupervisorOrOwnerStatus(orderId, target, actor, false);
    }

    @Transactional public CustomerOrder updateStatusBySupervisor(Long orderId, OrderStatus target, AppUser actor) {
        requireRole(actor, Role.SUPERVISOR);
        if (target != OrderStatus.PROCESSING && target != OrderStatus.CANCELLED) {
            throw ApiException.forbidden("Supervisors may only move an order to Processing or Cancelled. Delivery acceptance and completion belong to the assigned delivery person.");
        }
        return applySupervisorOrOwnerStatus(orderId, target, actor, true);
    }

    private CustomerOrder applySupervisorOrOwnerStatus(Long orderId, OrderStatus target, AppUser actor, boolean supervisor) {
        CustomerOrder order = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order not found."));
        OrderStatus current = order.getStatus();
        if (current == OrderStatus.COMPLETED || current == OrderStatus.CANCELLED) {
            throw ApiException.badRequest("A completed or cancelled order cannot be changed.");
        }
        if (target == OrderStatus.CONFIRMED && (supervisor || current != OrderStatus.PENDING)) {
            throw ApiException.badRequest("This order cannot be moved to Confirmed by this account.");
        }
        if (target == OrderStatus.PROCESSING && !(current == OrderStatus.PENDING || current == OrderStatus.CONFIRMED || current == OrderStatus.PARTIALLY_DELIVERED)) {
            throw ApiException.badRequest("This order cannot be moved to Processing from " + current + ".");
        }
        if (target == OrderStatus.CANCELLED) cancelBeforeDelivery(order);
        else if (target != OrderStatus.PROCESSING && target != OrderStatus.CONFIRMED) {
            throw ApiException.badRequest("Unsupported order status transition.");
        }
        order.setStatus(target);
        CustomerOrder saved = orders.save(order);
        saveHistory(saved, actor, current, target, (supervisor ? "Supervisor" : "Owner") + " changed order status to " + target + ".");
        notifyCustomer(saved, "ORDER_STATUS", "Order status updated", "Your order " + saved.getOrderNumber() + " is now " + friendlyStatus(target) + ".", target);
        return saved;
    }

    @Transactional public DeliveryAssignmentResponse createDispatch(Long orderId, DispatchRequest request, AppUser actor) {
        requireAnyRole(actor, Role.ADMIN, Role.SUPERVISOR);
        CustomerOrder order = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order not found."));
        if (!(order.getStatus() == OrderStatus.PROCESSING || order.getStatus() == OrderStatus.PARTIALLY_DELIVERED)) {
            throw ApiException.badRequest("Move the order to Processing before assigning a delivery.");
        }
        if (!assignments.findByOrder_IdAndStatusIn(orderId, ACTIVE_ASSIGNMENT_STATUSES).isEmpty()) {
            throw ApiException.conflict("This order already has a delivery assignment awaiting acceptance or completion.");
        }
        AppUser delivery = users.findById(request.deliveryUserId()).orElseThrow(() -> ApiException.notFound("Delivery account not found."));
        if (delivery.getRole() != Role.DELIVERY || !delivery.isEnabled()) throw ApiException.badRequest("Select an active account with the Delivery role.");

        Map<Long, Integer> alreadySentOrReserved = new HashMap<>();
        for (DeliveryAssignment prior : assignments.findByOrder_IdOrderByCreatedAtAsc(orderId)) {
            if (prior.getStatus() == DeliveryAssignmentStatus.CANCELLED) continue;
            for (DeliveryAssignmentItem line : prior.getItems()) {
                alreadySentOrReserved.merge(line.getOrderItem().getId(), line.getQuantity(), Integer::sum);
            }
        }
        Map<Long, OrderItem> orderItems = new LinkedHashMap<>();
        for (OrderItem item : order.getItems()) orderItems.put(item.getId(), item);
        Map<Long, Integer> toDispatch = new LinkedHashMap<>();
        if (Boolean.TRUE.equals(request.sendAllPendingItems())) {
            for (OrderItem item : order.getItems()) {
                int remaining = item.getQuantity() - alreadySentOrReserved.getOrDefault(item.getId(), 0);
                if (remaining > 0) toDispatch.put(item.getId(), remaining);
            }
        } else {
            if (request.items() == null || request.items().isEmpty()) throw ApiException.badRequest("Choose at least one item and quantity, or choose to dispatch all remaining items.");
            for (DispatchItemRequest line : request.items()) {
                if (toDispatch.containsKey(line.orderItemId())) throw ApiException.badRequest("An order item can only appear once in a dispatch request.");
                OrderItem item = orderItems.get(line.orderItemId());
                if (item == null) throw ApiException.badRequest("An item does not belong to this order.");
                int remaining = item.getQuantity() - alreadySentOrReserved.getOrDefault(item.getId(), 0);
                if (line.quantity() == null || line.quantity() < 1 || line.quantity() > remaining) {
                    throw ApiException.badRequest("Dispatch quantity exceeds the remaining quantity for " + item.getProductNameSnapshot() + ". Remaining: " + Math.max(remaining, 0) + ".");
                }
                toDispatch.put(item.getId(), line.quantity());
            }
        }
        if (toDispatch.isEmpty()) throw ApiException.badRequest("There are no remaining items available to dispatch.");

        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setOrder(order);
        assignment.setDeliveryUser(delivery);
        assignment.setAssignedBy(actor);
        assignment.setStatus(DeliveryAssignmentStatus.ASSIGNED);
        assignment.setNote(request.note() == null ? null : request.note().trim());
        List<DeliveryAssignmentItem> lines = new ArrayList<>();
        for (var entry : toDispatch.entrySet()) {
            DeliveryAssignmentItem line = new DeliveryAssignmentItem();
            line.setAssignment(assignment);
            line.setOrderItem(orderItems.get(entry.getKey()));
            line.setQuantity(entry.getValue());
            lines.add(line);
        }
        assignment.setItems(lines);
        DeliveryAssignment saved = assignments.saveAndFlush(assignment);
        saveHistory(order, actor, order.getStatus(), order.getStatus(), "Delivery assignment #" + saved.getId() + " assigned to " + delivery.getName() + ".");
        events.publishEvent(new OrderWorkflowEvent("DELIVERY_ASSIGNED", order.getId(), order.getOrderNumber(), saved.getId(), delivery.getId(), null,
            "New delivery assigned", "Order " + order.getOrderNumber() + " has been assigned to you. Open Nabta to review and accept it.", "ASSIGNED"));
        return assignmentDto(saved);
    }

    @Transactional public DeliveryAssignmentResponse cancelDispatch(Long assignmentId, AppUser actor) {
        requireAnyRole(actor, Role.ADMIN, Role.SUPERVISOR);
        DeliveryAssignment assignment = assignments.findById(assignmentId).orElseThrow(() -> ApiException.notFound("Delivery assignment not found."));
        if (assignment.getStatus() != DeliveryAssignmentStatus.ASSIGNED) throw ApiException.badRequest("Only an assignment that has not yet been accepted can be cancelled or reassigned.");
        assignment.setStatus(DeliveryAssignmentStatus.CANCELLED);
        assignments.save(assignment);
        CustomerOrder order = assignment.getOrder();
        saveHistory(order, actor, order.getStatus(), order.getStatus(), "Unaccepted delivery assignment #" + assignment.getId() + " was cancelled; remaining items are available for reassignment.");
        events.publishEvent(new OrderWorkflowEvent("DELIVERY_ASSIGNMENT_CANCELLED", order.getId(), order.getOrderNumber(), assignment.getId(), assignment.getDeliveryUser().getId(), null,
            "Delivery assignment cancelled", "The delivery assignment for order " + order.getOrderNumber() + " was cancelled.", "CANCELLED"));
        return assignmentDto(assignment);
    }

    @Transactional public DeliveryAssignmentResponse acceptDispatch(Long assignmentId, AppUser delivery) {
        requireRole(delivery, Role.DELIVERY);
        DeliveryAssignment assignment = assignments.findByIdAndDeliveryUser_Id(assignmentId, delivery.getId()).orElseThrow(() -> ApiException.notFound("This delivery assignment was not found for your account."));
        if (assignment.getStatus() != DeliveryAssignmentStatus.ASSIGNED) throw ApiException.badRequest("Only a new assignment can be accepted.");
        CustomerOrder order = assignment.getOrder();
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) throw ApiException.conflict("This order is no longer available for delivery.");
        assignment.setStatus(DeliveryAssignmentStatus.ACCEPTED);
        assignment.setAcceptedAt(LocalDateTime.now());
        assignments.save(assignment);
        OrderStatus previous = order.getStatus();
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        orders.save(order);
        saveHistory(order, delivery, previous, OrderStatus.OUT_FOR_DELIVERY, delivery.getName() + " accepted delivery assignment #" + assignment.getId() + ".");
        notifyCustomer(order, "OUT_FOR_DELIVERY", "Your order is out for delivery", "Your order " + order.getOrderNumber() + " has been accepted by the delivery person and is out for delivery.", OrderStatus.OUT_FOR_DELIVERY);
        return assignmentDto(assignment);
    }

    @Transactional public DeliveryAssignmentResponse completeDispatch(Long assignmentId, AppUser delivery) {
        requireRole(delivery, Role.DELIVERY);
        DeliveryAssignment assignment = assignments.findByIdAndDeliveryUser_Id(assignmentId, delivery.getId()).orElseThrow(() -> ApiException.notFound("This delivery assignment was not found for your account."));
        if (assignment.getStatus() != DeliveryAssignmentStatus.ACCEPTED) throw ApiException.badRequest("Accept the delivery assignment before marking it delivered.");
        assignment.setStatus(DeliveryAssignmentStatus.DELIVERED);
        assignment.setDeliveredAt(LocalDateTime.now());
        assignments.save(assignment);
        CustomerOrder order = assignment.getOrder();
        Map<Long, Integer> delivered = new HashMap<>();
        for (DeliveryAssignment prior : assignments.findByOrder_IdOrderByCreatedAtAsc(order.getId())) {
            if (prior.getStatus() != DeliveryAssignmentStatus.DELIVERED) continue;
            for (DeliveryAssignmentItem line : prior.getItems()) delivered.merge(line.getOrderItem().getId(), line.getQuantity(), Integer::sum);
        }
        boolean fullyDelivered = order.getItems().stream().allMatch(item -> delivered.getOrDefault(item.getId(), 0) >= item.getQuantity());
        OrderStatus previous = order.getStatus();
        OrderStatus next = fullyDelivered ? OrderStatus.COMPLETED : OrderStatus.PARTIALLY_DELIVERED;
        order.setStatus(next);
        orders.save(order);
        saveHistory(order, delivery, previous, next, delivery.getName() + " marked delivery assignment #" + assignment.getId() + " as delivered.");
        if (fullyDelivered) {
            notifyCustomer(order, "ORDER_DELIVERED", "Your Nabta order has been delivered", "All items in order " + order.getOrderNumber() + " have been delivered.", OrderStatus.COMPLETED);
        } else {
            notifyCustomer(order, "PARTIAL_DELIVERY", "Your Nabta delivery is complete", "Part of order " + order.getOrderNumber() + " was delivered. The remaining items are still pending.", OrderStatus.PARTIALLY_DELIVERED);
        }
        return assignmentDto(assignment);
    }

    @Transactional(readOnly = true) public Page<DeliveryAssignmentResponse> deliveryAssignments(AppUser actor, DeliveryAssignmentStatus status, Pageable pageable) {
        requireRole(actor, Role.DELIVERY);
        Page<DeliveryAssignment> result = status == null
            ? assignments.findByDeliveryUser_IdOrderByCreatedAtDesc(actor.getId(), pageable)
            : assignments.findByDeliveryUser_IdAndStatusOrderByCreatedAtDesc(actor.getId(), status, pageable);
        return result.map(this::assignmentDto);
    }

    @Transactional(readOnly = true) public Page<DeliveryAssignmentResponse> operationsAssignments(AppUser actor, DeliveryAssignmentStatus status, Pageable pageable) {
        requireAnyRole(actor, Role.ADMIN, Role.SUPERVISOR);
        Page<DeliveryAssignment> result = status == null ? assignments.findAllByOrderByCreatedAtDesc(pageable) : assignments.findByStatusOrderByCreatedAtDesc(status, pageable);
        return result.map(this::assignmentDto);
    }

    @Transactional(readOnly = true) public DeliveryAssignmentResponse deliveryAssignment(Long assignmentId, AppUser actor) {
        requireRole(actor, Role.DELIVERY);
        DeliveryAssignment assignment = assignments.findByIdAndDeliveryUser_Id(assignmentId, actor.getId()).orElseThrow(() -> ApiException.notFound("This delivery assignment was not found for your account."));
        return assignmentDto(assignment);
    }

    @Transactional(readOnly = true) public List<OrderStatusHistoryResponse> customerHistory(Long orderId, Long userId) {
        CustomerOrder order = orders.findByIdAndUser_Id(orderId, userId).orElseThrow(() -> ApiException.notFound("Order not found."));
        return history.findByOrder_IdOrderByCreatedAtAsc(order.getId()).stream().map(this::historyDto).toList();
    }

    @Transactional(readOnly = true) public List<DeliveryAssignmentResponse> customerDeliveries(Long orderId, Long userId) {
        CustomerOrder order = orders.findByIdAndUser_Id(orderId, userId).orElseThrow(() -> ApiException.notFound("Order not found."));
        return assignments.findByOrder_IdOrderByCreatedAtAsc(order.getId()).stream().map(this::assignmentDto).toList();
    }

    @Transactional(readOnly = true) public List<OrderStatusHistoryResponse> operationsHistory(Long orderId, AppUser actor) {
        requireAnyRole(actor, Role.ADMIN, Role.SUPERVISOR);
        CustomerOrder order = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order not found."));
        return history.findByOrder_IdOrderByCreatedAtAsc(order.getId()).stream().map(this::historyDto).toList();
    }

    private void cancelBeforeDelivery(CustomerOrder order) {
        if (order.getStatus() == OrderStatus.OUT_FOR_DELIVERY || order.getStatus() == OrderStatus.PARTIALLY_DELIVERED) {
            throw ApiException.conflict("An order that is out for delivery or partially delivered cannot be cancelled through this action.");
        }
        List<DeliveryAssignment> prior = assignments.findByOrder_IdOrderByCreatedAtAsc(order.getId());
        if (prior.stream().anyMatch(a -> a.getStatus() == DeliveryAssignmentStatus.ACCEPTED || a.getStatus() == DeliveryAssignmentStatus.DELIVERED)) {
            throw ApiException.conflict("This order has an accepted or completed delivery and cannot be cancelled this way.");
        }
        for (DeliveryAssignment assignment : prior) {
            if (assignment.getStatus() == DeliveryAssignmentStatus.ASSIGNED) assignment.setStatus(DeliveryAssignmentStatus.CANCELLED);
        }
        for (OrderItem item : order.getItems()) {
            Product product = products.findByIdForUpdate(item.getProduct().getId()).orElseThrow(() -> ApiException.conflict("An order product is missing; stock could not be restored automatically."));
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
        }
    }

    private DeliveryAssignmentResponse assignmentDto(DeliveryAssignment a) {
        List<DeliveryAssignmentItemResponse> lines = a.getItems().stream().map(line -> {
            OrderItem oi = line.getOrderItem();
            Product product = oi.getProduct();
            return new DeliveryAssignmentItemResponse(oi.getId(), product.getId(), oi.getProductNameSnapshot(), oi.getProductNameArSnapshot(), product.getImageUrl(), oi.getQuantity(), line.getQuantity(), oi.getUnitPriceSnapshot());
        }).toList();
        return new DeliveryAssignmentResponse(a.getId(), a.getOrder().getId(), a.getOrder().getOrderNumber(), a.getDeliveryUser().getId(), a.getDeliveryUser().getName(), a.getAssignedBy().getName(), a.getStatus(), lines, a.getNote(), a.getCreatedAt(), a.getAcceptedAt(), a.getDeliveredAt());
    }

    private OrderStatusHistoryResponse historyDto(OrderStatusHistory h) {
        AppUser actor = h.getActor();
        return new OrderStatusHistoryResponse(h.getId(), h.getPreviousStatus(), h.getNewStatus(), h.getNote(), actor == null ? "System" : actor.getName(), actor == null ? null : actor.getRole(), h.getCreatedAt());
    }

    private void saveHistory(CustomerOrder order, AppUser actor, OrderStatus previous, OrderStatus next, String note) {
        OrderStatusHistory entry = new OrderStatusHistory();
        entry.setOrder(order); entry.setActor(actor); entry.setPreviousStatus(previous); entry.setNewStatus(next); entry.setNote(note);
        history.save(entry);
    }

    private void notifyCustomer(CustomerOrder order, String type, String title, String body, OrderStatus status) {
        events.publishEvent(new OrderWorkflowEvent(type, order.getId(), order.getOrderNumber(), null, order.getUser().getId(), null, title, body, status.name()));
    }

    private String friendlyStatus(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Pending"; case CONFIRMED -> "Confirmed"; case PROCESSING -> "Processing";
            case OUT_FOR_DELIVERY -> "Out for Delivery"; case PARTIALLY_DELIVERED -> "Partially Delivered";
            case COMPLETED -> "Completed"; case CANCELLED -> "Cancelled";
        };
    }

    private void requireRole(AppUser user, Role role) {
        if (user == null || user.getRole() != role) throw ApiException.forbidden("Your account does not have permission to perform this action.");
    }
    private void requireAnyRole(AppUser user, Role... allowed) {
        if (user == null || Arrays.stream(allowed).noneMatch(role -> role == user.getRole())) throw ApiException.forbidden("Your account does not have permission to perform this action.");
    }
}
