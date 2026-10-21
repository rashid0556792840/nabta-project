package com.nabta.backend.service;
import com.nabta.backend.dto.CatalogDtos.*;
import com.nabta.backend.exception.ApiException;
import com.nabta.backend.model.Category;
import com.nabta.backend.model.Product;
import com.nabta.backend.repository.CategoryRepository;
import com.nabta.backend.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
@Service
public class CatalogService {
    private final ProductRepository products; private final CategoryRepository categories;
    public CatalogService(ProductRepository products, CategoryRepository categories) { this.products=products; this.categories=categories; }
    @Transactional(readOnly=true) public Page<ProductResponse> products(String q, Long categoryId, boolean admin, Pageable pageable) { return products.search(!admin, categoryId, clean(q), pageable).map(this::productDto); }
    @Transactional(readOnly=true) public ProductResponse getProduct(Long id, boolean admin) { Product p=products.findById(id).orElseThrow(() -> ApiException.notFound("Product not found.")); if (!admin && (!p.isActive() || !p.getCategory().isActive())) throw ApiException.notFound("Product not found."); return productDto(p); }
    @Transactional(readOnly=true) public List<CategoryResponse> categories(boolean admin) { return (admin ? categories.findAll() : categories.findByActiveTrueOrderByNameAsc()).stream().map(this::categoryDto).toList(); }
    @Transactional(readOnly=true) public List<ProductResponse> productsForCategory(Long categoryId, Pageable pageable) { Category category=categories.findById(categoryId).orElseThrow(() -> ApiException.notFound("Category not found.")); if (!category.isActive()) throw ApiException.notFound("Category not found."); return products.search(true,categoryId,null,pageable).getContent().stream().map(this::productDto).toList(); }
    @Transactional public CategoryResponse createCategory(CategoryRequest r) { String name=r.name().trim(); if (categories.existsByNameIgnoreCase(name)) throw ApiException.conflict("A category with this English name already exists."); Category c=new Category(); updateCategoryFields(c,r); return categoryDto(categories.save(c)); }
    @Transactional public CategoryResponse updateCategory(Long id, CategoryRequest r) { Category c=categories.findById(id).orElseThrow(() -> ApiException.notFound("Category not found.")); categories.findAll().stream().filter(other -> !other.getId().equals(id) && other.getName().equalsIgnoreCase(r.name().trim())).findFirst().ifPresent(other -> { throw ApiException.conflict("A category with this English name already exists."); }); updateCategoryFields(c,r); return categoryDto(categories.save(c)); }
    @Transactional public void deactivateCategory(Long id) { Category c=categories.findById(id).orElseThrow(() -> ApiException.notFound("Category not found.")); c.setActive(false); categories.save(c); }
    @Transactional public ProductResponse createProduct(ProductRequest r) { Product p=new Product(); applyProduct(p,r); return productDto(products.save(p)); }
    @Transactional public ProductResponse updateProduct(Long id, ProductRequest r) { Product p=products.findById(id).orElseThrow(() -> ApiException.notFound("Product not found.")); applyProduct(p,r); return productDto(products.save(p)); }
    @Transactional public void deactivateProduct(Long id) { Product p=products.findById(id).orElseThrow(() -> ApiException.notFound("Product not found.")); p.setActive(false); products.save(p); }
    @Transactional public ProductResponse changeStock(Long id, int quantity) { if (quantity < 0) throw ApiException.badRequest("Stock quantity cannot be negative."); Product p=products.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Product not found.")); p.setStockQuantity(quantity); return productDto(products.save(p)); }
    @Transactional(readOnly=true) public List<InventoryItem> inventory(int lowStockThreshold) { return products.findAll().stream().map(p -> new InventoryItem(p.getId(),p.getName(),p.getNameAr(),p.getStockQuantity(),p.isActive(),p.isActive() && p.getStockQuantity() <= lowStockThreshold)).toList(); }
    public record InventoryItem(Long productId, String productName, String productNameAr, int stockQuantity, boolean active, boolean lowStock) {}
    private void applyProduct(Product p, ProductRequest r) {
        Category c=categories.findById(r.categoryId()).orElseThrow(() -> ApiException.badRequest("Selected category does not exist."));
        if (!c.isActive()) throw ApiException.badRequest("Choose an active category.");
        p.setName(r.name().trim()); p.setNameAr(clean(r.nameAr())); p.setDescription(clean(r.description())); p.setDescriptionAr(clean(r.descriptionAr()));
        p.setPrice(r.price().setScale(2, java.math.RoundingMode.HALF_UP)); p.setImageUrl(r.imageUrl()); p.setCategory(c); p.setStockQuantity(r.stockQuantity()); if (r.active()!=null) p.setActive(r.active());
    }
    private void updateCategoryFields(Category c, CategoryRequest r) { c.setName(r.name().trim()); c.setNameAr(clean(r.nameAr())); c.setDescription(clean(r.description())); c.setDescriptionAr(clean(r.descriptionAr())); c.setImageUrl(r.imageUrl()); if (r.active()!=null) c.setActive(r.active()); }
    private ProductResponse productDto(Product p) { return new ProductResponse(p.getId(),p.getName(),p.getNameAr(),p.getDescription(),p.getDescriptionAr(),p.getPrice(),p.getImageUrl(),p.getCategory().getId(),p.getCategory().getName(),p.getCategory().getNameAr(),p.getStockQuantity(),p.isActive() && p.getStockQuantity()>0,p.isActive()); }
    private CategoryResponse categoryDto(Category c) { return new CategoryResponse(c.getId(),c.getName(),c.getNameAr(),c.getDescription(),c.getDescriptionAr(),c.getImageUrl(),c.isActive()); }
    private String clean(String s) { if (s==null || s.isBlank()) return null; return s.trim(); }
}
