package com.safar_zone_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(columnList = "email", unique = true),  // ✅ Faster + unique constraint
        @Index(columnList = "phone"),                  // ✅ Faster phone lookup
        @Index(columnList = "role")                    // ✅ Faster role-based queries
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String email;  // ✅ Store normalized

    @Column(nullable = false)
    private String password;  // ✅ BCrypt hashed

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriverVerificationStatus driverVerificationStatus =
            DriverVerificationStatus.NOT_SUBMITTED;

    // ✅ Use wrapper Boolean for null-safety
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    // ✅ Hibernate auto-manages timestamps
    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column
    private String licenseNumber;

    // ✅ Helper: Check if user has specific role
    public boolean hasRole(Role requiredRole) {
        return this.role == requiredRole;
    }
}