package com.nabta.backend.service;
import com.nabta.backend.dto.OrderDtos.*;
import com.nabta.backend.event.OrderCreatedEvent;
import com.nabta.backend.exception.ApiException;
import com.nabta.backend.model.*;
import com.nabta.backend.repository.OrderRepository;
import com.nabta.backend.repository.OrderStatusHistoryRepository;
import com.nabta.backend.repository.ProductRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
@Service
public class OrderService {
    private final OrderRepository orders; private final ProductRepository products; private final OrderStatusHistoryRepository history; private final ApplicationEventPublisher events; private final PaymentReadinessService payments;
    public OrderService(OrderRepository orders, ProductRepository products, OrderStatusHistoryRepository history, ApplicationEventPublisher events, PaymentReadinessService payments) { this.orders=orders; this.products=products; this.history=history; this.events=events; this.payments=payments; }
    @Transactional public OrderResponse create(AppUser user, CreateOrderRequest request, String idempotencyKey) {
        // Cash on delivery is active while no payment provider is connected.
        // Never accept an online method until a real provider adapter and server-side payment verification exist.
        PaymentMethod requestedMethod = request.paymentMethod() == null ? PaymentMethod.CASH_ON_DELIVERY : request.paymentMethod();
        if (requestedMethod != PaymentMethod.CASH_ON_DELIVERY) {
            throw ApiException.conflict("Online payments are not connected yet. Please choose Cash on Delivery.");
        }
        String key=normalizeKey(idempotencyKey);
        if (key!=null) { var old=orders.findByUser_IdAndIdempotencyKey(user.getId(),key); if (old.isPresent()) return toDto(old.get()); }
        CustomerOrder order=new CustomerOrder(); order.setUser(user); order.setOrderNumber("NBT-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT)); order.setStatus(OrderStatus.PENDING); order.setDeliveryAddress(request.deliveryAddress().trim()); order.setContactPhone(request.contactPhone().trim()); order.setNotes(request.notes()); order.setPaymentMethod(requestedMethod); order.setIdempotencyKey(key); order.setDeliveryCharge(BigDecimal.ZERO.setScale(2));
        BigDecimal subtotal=BigDecimal.ZERO; List<OrderItem> items=new ArrayList<>();
        for (OrderItemRequest line:request.items()) {
            Product product=products.findByIdForUpdate(line.productId()).orElseThrow(() -> ApiException.badRequest("One of the selected products does not exist."));
            if (!product.isActive() || !product.getCategory().isActive()) throw ApiException.badRequest("Product '"+product.getName()+"' is not available.");
            if (line.quantity()>product.getStockQuantity()) throw ApiException.conflict("Not enough stock for '"+product.getName()+"'. Available quantity: "+product.getStockQuantity()+".");
            BigDecimal unit=product.getPrice(); BigDecimal lineTotal=unit.multiply(BigDecimal.valueOf(line.quantity())).setScale(2,RoundingMode.HALF_UP); subtotal=subtotal.add(lineTotal);
            product.setStockQuantity(product.getStockQuantity()-line.quantity());
            OrderItem item=new OrderItem(); item.setOrder(order); item.setProduct(product); item.setProductNameSnapshot(product.getName()); item.setProductNameArSnapshot(product.getNameAr()); item.setUnitPriceSnapshot(unit); item.setQuantity(line.quantity()); item.setLineTotal(lineTotal); items.add(item);
        }
        order.setSubtotal(subtotal.setScale(2,RoundingMode.HALF_UP)); order.setTotalAmount(order.getSubtotal().add(order.getDeliveryCharge())); order.setItems(items);
        CustomerOrder saved = orders.saveAndFlush(order);
        OrderStatusHistory initialHistory = new OrderStatusHistory(); initialHistory.setOrder(saved); initialHistory.setActor(user); initialHistory.setPreviousStatus(null); initialHistory.setNewStatus(OrderStatus.PENDING); initialHistory.setNote("Order placed by customer."); history.save(initialHistory);
        // Push delivery is handled only after this transaction commits.
        events.publishEvent(new OrderCreatedEvent(saved.getId(), saved.getOrderNumber()));
        return toDto(saved);
    }
    @Transactional(readOnly=true) public Page<OrderResponse> myOrders(Long userId, Pageable pageable) { return orders.findByUser_IdOrderByCreatedAtDesc(userId,pageable).map(this::toDto); }
    @Transactional(readOnly=true) public OrderResponse myOrder(Long orderId, Long userId) { CustomerOrder o=orders.findByIdAndUser_Id(orderId,userId).orElseThrow(() -> ApiException.notFound("Order not found.")); return toDto(o); }
    @Transactional(readOnly=true) public Page<OrderResponse> adminOrders(OrderStatus status, Pageable pageable) { return (status==null ? orders.findAllByOrderByCreatedAtDesc(pageable) : orders.findByStatusOrderByCreatedAtDesc(status,pageable)).map(this::toDto); }
    @Transactional(readOnly=true) public OrderResponse operationsOrder(Long id) {
        CustomerOrder order=orders.findById(id).orElseThrow(() -> ApiException.notFound("Order not found."));
        return toDto(order);
    }
    private OrderResponse toDto(CustomerOrder o) {
        List<OrderItemResponse> itemDtos=o.getItems().stream().map(i -> new OrderItemResponse(i.getId(),i.getProduct().getId(),i.getProductNameSnapshot(),i.getProductNameArSnapshot(),i.getProduct().getImageUrl(),i.getUnitPriceSnapshot(),i.getQuantity(),i.getLineTotal())).toList();
        return new OrderResponse(o.getId(),o.getOrderNumber(),o.getStatus(),o.getSubtotal(),o.getDeliveryCharge(),o.getTotalAmount(),o.getDeliveryAddress(),o.getContactPhone(),o.getNotes(),o.getPaymentMethod(),itemDtos,o.getCreatedAt(),o.getUser().getId(),o.getUser().getName(),o.getUser().getEmail());
    }
    private String normalizeKey(String key) { if (key==null || key.isBlank()) return null; String k=key.trim(); if (k.length()>80) throw ApiException.badRequest("Idempotency-Key must not exceed 80 characters."); return k; }
}
