package com.nabta.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "device_tokens", uniqueConstraints = @UniqueConstraint(name = "uk_device_tokens_fcm_token", columnNames = "fcm_token"))
public class DeviceToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_device_tokens_user"))
    private AppUser user;

    @Column(name = "fcm_token", nullable = false, length = 500, unique = true)
    private String fcmToken;

    @Column(nullable = false, length = 20)
    private String platform = "ANDROID";

    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
