package com.nabta.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "order_status_history", indexes = @Index(name = "idx_order_status_history_order_created", columnList = "order_id,created_at"))
public class OrderStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false) private CustomerOrder order;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "actor_user_id") private AppUser actor;
    @Enumerated(EnumType.STRING) @Column(name = "previous_status", length = 30) private OrderStatus previousStatus;
    @Enumerated(EnumType.STRING) @Column(name = "new_status", nullable = false, length = 30) private OrderStatus newStatus;
    @Column(nullable = false, length = 1000) private String note;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
}
