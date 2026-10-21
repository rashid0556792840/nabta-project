package com.nabta.backend.repository;

import com.nabta.backend.model.DeliveryAssignment;
import com.nabta.backend.model.DeliveryAssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {
    List<DeliveryAssignment> findByOrder_IdOrderByCreatedAtAsc(Long orderId);
    List<DeliveryAssignment> findByOrder_IdAndStatusIn(Long orderId, Collection<DeliveryAssignmentStatus> statuses);
    Optional<DeliveryAssignment> findByIdAndDeliveryUser_Id(Long id, Long deliveryUserId);
    Page<DeliveryAssignment> findByDeliveryUser_IdOrderByCreatedAtDesc(Long deliveryUserId, Pageable pageable);
    Page<DeliveryAssignment> findByDeliveryUser_IdAndStatusOrderByCreatedAtDesc(Long deliveryUserId, DeliveryAssignmentStatus status, Pageable pageable);
    Page<DeliveryAssignment> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<DeliveryAssignment> findByStatusOrderByCreatedAtDesc(DeliveryAssignmentStatus status, Pageable pageable);
}
