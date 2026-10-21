package com.nabta.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "customer_orders", indexes = {@Index(name = "idx_orders_user_created", columnList = "user_id,created_at"), @Index(name = "idx_orders_status", columnList = "status")}, uniqueConstraints = @UniqueConstraint(name = "uk_order_user_idempotency", columnNames = {"user_id", "idempotency_key"}))
public class CustomerOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "order_number", nullable = false, unique = true, length = 40) private String orderNumber;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private AppUser user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private OrderStatus status = OrderStatus.PENDING;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal subtotal;
    @Column(name = "delivery_charge", nullable = false, precision = 10, scale = 2) private BigDecimal deliveryCharge = BigDecimal.ZERO;
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2) private BigDecimal totalAmount;
    @Column(name = "delivery_address", nullable = false, length = 1500) private String deliveryAddress;
    @Column(name = "contact_phone", nullable = false, length = 40) private String contactPhone;
    @Column(name = "order_notes", length = 1500) private String notes;
    @Enumerated(EnumType.STRING) @Column(name = "payment_method", nullable = false, length = 30) private PaymentMethod paymentMethod = PaymentMethod.CASH_ON_DELIVERY;
    @Column(name = "idempotency_key", length = 80) private String idempotencyKey;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true) private List<OrderItem> items = new ArrayList<>();
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}
