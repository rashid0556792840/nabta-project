package com.nabta.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "delivery_assignment_items", uniqueConstraints = @UniqueConstraint(name = "uk_delivery_assignment_order_item", columnNames = {"assignment_id", "order_item_id"}))
public class DeliveryAssignmentItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "assignment_id", nullable = false) private DeliveryAssignment assignment;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_item_id", nullable = false) private OrderItem orderItem;
    @Column(nullable = false) private int quantity;
}
