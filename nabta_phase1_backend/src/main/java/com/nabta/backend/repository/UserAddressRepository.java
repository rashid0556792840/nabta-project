package com.nabta.backend.repository;
import com.nabta.backend.model.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findByUser_IdOrderByCreatedAtDesc(Long userId);
    Optional<UserAddress> findByIdAndUser_Id(Long id, Long userId);
}
