package com.nabta.backend.repository;

import com.nabta.backend.model.DeviceToken;
import com.nabta.backend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    Optional<DeviceToken> findByFcmToken(String fcmToken);

    @Query("select d from DeviceToken d join fetch d.user u where u.id = :userId")
    List<DeviceToken> findAllByUserId(@Param("userId") Long userId);

    @Query("select d from DeviceToken d join fetch d.user u where u.role = :role")
    List<DeviceToken> findAllByUserRole(@Param("role") Role role);

    @Query("select count(d) from DeviceToken d join d.user u where u.role = :role")
    long countByUserRole(@Param("role") Role role);

    @Modifying
    @Query("delete from DeviceToken d where d.user.id = :userId and d.fcmToken = :token")
    void deleteByUserIdAndFcmToken(@Param("userId") Long userId, @Param("token") String token);
}
