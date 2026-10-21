package com.nabta.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "categories", uniqueConstraints = @UniqueConstraint(name = "uk_categories_name", columnNames = "name"))
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    /** Legacy `name` column stores the English name to preserve existing records/API clients. */
    @Column(nullable = false, length = 100) private String name;
    @Column(name = "name_ar", length = 100) private String nameAr;
    @Column(length = 1000) private String description;
    @Column(name = "description_ar", length = 1000) private String descriptionAr;
    @Column(name = "image_url", length = 1000) private String imageUrl;
    @Column(name = "is_active", nullable = false) private boolean active = true;
}
