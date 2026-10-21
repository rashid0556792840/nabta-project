package com.nabta.backend.repository;
import com.nabta.backend.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {}
