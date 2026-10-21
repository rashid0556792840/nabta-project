package com.nabta.backend.repository;
import com.nabta.backend.model.CustomerOrder;
import com.nabta.backend.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.Optional;
public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    Page<CustomerOrder> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Optional<CustomerOrder> findByIdAndUser_Id(Long id, Long userId);
    Optional<CustomerOrder> findByUser_IdAndIdempotencyKey(Long userId, String idempotencyKey);
    Page<CustomerOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<CustomerOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);
    long countByStatus(OrderStatus status);
    long countByUser_Role(com.nabta.backend.model.Role role);
    @Query("select coalesce(sum(o.totalAmount), 0) from CustomerOrder o where o.status = :status") BigDecimal sumTotalsByStatus(@Param("status") OrderStatus status);
}
