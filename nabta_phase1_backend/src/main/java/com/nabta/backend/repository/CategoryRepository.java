package com.nabta.backend.repository;
import com.nabta.backend.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNameIgnoreCase(String name);
    List<Category> findByActiveTrueOrderByNameAsc();
}
