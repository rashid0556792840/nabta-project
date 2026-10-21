package com.nabta.backend.repository;
import com.nabta.backend.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("select p from Product p where (:activeOnly = false or (p.active = true and p.category.active = true)) and (:categoryId is null or p.category.id = :categoryId) and (:q is null or lower(p.name) like lower(concat('%', :q, '%')) or lower(coalesce(p.nameAr, '')) like lower(concat('%', :q, '%')) or lower(coalesce(p.description, '')) like lower(concat('%', :q, '%')) or lower(coalesce(p.descriptionAr, '')) like lower(concat('%', :q, '%')) or lower(coalesce(p.category.nameAr, '')) like lower(concat('%', :q, '%')) or lower(p.category.name) like lower(concat('%', :q, '%')))" )
    Page<Product> search(@Param("activeOnly") boolean activeOnly, @Param("categoryId") Long categoryId, @Param("q") String q, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from Product p where p.id = :id") Optional<Product> findByIdForUpdate(@Param("id") Long id);
    long countByActiveTrue();
    long countByStockQuantityLessThanAndActiveTrue(int quantity);
}
