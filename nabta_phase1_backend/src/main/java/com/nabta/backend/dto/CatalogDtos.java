package com.nabta.backend.dto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
public final class CatalogDtos {
    private CatalogDtos() {}
    /** name/description remain the English legacy fields; Arabic values are stored separately. */
    public record CategoryRequest(@NotBlank @Size(max=100) String name, @Size(max=100) String nameAr,
                                  @Size(max=1000) String description, @Size(max=1000) String descriptionAr,
                                  @Size(max=1000) String imageUrl, Boolean active) {}
    public record CategoryResponse(Long id, String name, String nameAr, String description, String descriptionAr,
                                   String imageUrl, boolean active) {}
    public record ProductRequest(@NotBlank @Size(max=150) String name, @Size(max=150) String nameAr,
                                 @Size(max=3000) String description, @Size(max=3000) String descriptionAr,
                                 @NotNull @DecimalMin(value="0.01") BigDecimal price, @Size(max=1000) String imageUrl,
                                 @NotNull Long categoryId, @NotNull @Min(0) Integer stockQuantity, Boolean active) {}
    public record ProductResponse(Long id, String name, String nameAr, String description, String descriptionAr,
                                  BigDecimal price, String imageUrl, Long categoryId, String categoryName,
                                  String categoryNameAr, int stockQuantity, boolean available, boolean active) {}
}
