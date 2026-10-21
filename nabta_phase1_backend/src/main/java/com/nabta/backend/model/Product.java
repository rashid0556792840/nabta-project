package com.nabta.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "products", indexes = {@Index(name = "idx_products_active", columnList = "is_active"), @Index(name = "idx_products_category", columnList = "category_id")})
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    /** Legacy `name` column stores the English name to preserve existing records/API clients. */
    @Column(nullable = false, length = 150) private String name;
    @Column(name = "name_ar", length = 150) private String nameAr;
    @Column(length = 3000) private String description;
    @Column(name = "description_ar", length = 3000) private String descriptionAr;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal price;
    @Column(name = "image_url", length = 1000) private String imageUrl;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "category_id", nullable = false) private Category category;
    @Column(name = "stock_quantity", nullable = false) private int stockQuantity;
    @Column(name = "is_active", nullable = false) private boolean active = true;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}
