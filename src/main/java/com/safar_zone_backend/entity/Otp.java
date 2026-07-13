package com.safar_zone_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "otps", indexes = {
        @Index(columnList = "email"),           // ✅ Faster lookup by email
        @Index(columnList = "expires_at")       // ✅ Faster cleanup of expired OTPs
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Otp {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String email;  // ✅ Store normalized (lowercase + trimmed)

    @Column(name = "otp_code", nullable = false)
    private String otpCode;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // ✅ Use wrapper Boolean for JPA null-safety
    @Column(nullable = false)
    private Boolean used = false;

    // ✅ Track creation time for rate limiting
    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    // ✅ Helper: Check if OTP is still valid
    public boolean isValid() {
        return !Boolean.TRUE.equals(used) &&
                expiresAt != null &&
                !expiresAt.isBefore(LocalDateTime.now());
    }
}