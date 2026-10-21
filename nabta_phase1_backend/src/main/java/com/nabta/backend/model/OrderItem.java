package com.nabta.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "order_items", indexes = @Index(name = "idx_order_items_order", columnList = "order_id"))
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false) private CustomerOrder order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id", nullable = false) private Product product;
    @Column(name = "product_name_snapshot", nullable = false, length = 150) private String productNameSnapshot;
    @Column(name = "product_name_ar_snapshot", length = 150) private String productNameArSnapshot;
    @Column(name = "unit_price_snapshot", nullable = false, precision = 10, scale = 2) private BigDecimal unitPriceSnapshot;
    @Column(nullable = false) private int quantity;
    @Column(name = "line_total", nullable = false, precision = 10, scale = 2) private BigDecimal lineTotal;
}
