package com.nabta.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "delivery_assignments", indexes = {
    @Index(name = "idx_delivery_assignment_order", columnList = "order_id"),
    @Index(name = "idx_delivery_assignment_user_status", columnList = "delivery_user_id,status")
})
public class DeliveryAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false) private CustomerOrder order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "delivery_user_id", nullable = false) private AppUser deliveryUser;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "assigned_by_user_id", nullable = false) private AppUser assignedBy;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private DeliveryAssignmentStatus status = DeliveryAssignmentStatus.ASSIGNED;
    @Column(length = 1000) private String note;
    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true) private List<DeliveryAssignmentItem> items = new ArrayList<>();
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @Column(name = "accepted_at") private LocalDateTime acceptedAt;
    @Column(name = "delivered_at") private LocalDateTime deliveredAt;
}
