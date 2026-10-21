package com.nabta.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Persisted payment-attempt foundation. No real attempts are created until a provider is integrated. */
@Getter @Setter @NoArgsConstructor
@Entity
@Table(
    name = "payment_transactions",
    indexes = {
        @Index(name = "idx_payment_order_status", columnList = "order_id,status"),
        @Index(name = "idx_payment_created", columnList = "created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_merchant_reference", columnNames = "merchant_reference"),
        @UniqueConstraint(name = "uk_payment_provider_id", columnNames = {"provider_code", "provider_payment_id"})
    }
)
public class PaymentTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id") private CustomerOrder order;
    @Column(name = "merchant_reference", nullable = false, length = 80) private String merchantReference;
    @Column(name = "provider_code", nullable = false, length = 40) private String providerCode = "NONE";
    @Column(name = "provider_payment_id", length = 160) private String providerPaymentId;
    @Enumerated(EnumType.STRING) @Column(name = "payment_method", length = 30) private PaymentMethod paymentMethod;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency = "SAR";
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private PaymentStatus status = PaymentStatus.CREATED;
    @Column(name = "provider_status", length = 80) private String providerStatus;
    @Column(name = "failure_code", length = 120) private String failureCode;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
    @Column(name = "verified_at") private LocalDateTime verifiedAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}
