package com.nabta.backend.repository;
import com.nabta.backend.model.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {
    List<UserFavorite> findByUser_IdOrderByCreatedAtDesc(Long userId);
    Optional<UserFavorite> findByUser_IdAndProduct_Id(Long userId, Long productId);
    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);
}
