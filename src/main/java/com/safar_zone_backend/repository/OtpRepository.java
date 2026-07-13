package com.safar_zone_backend.repository;

import com.safar_zone_backend.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, String> {

    // ==================== 🔹 BASIC QUERIES ====================

    Optional<Otp> findByEmailAndOtpCode(@Param("email") String email, @Param("otpCode") String otpCode);

    List<Otp> findByEmail(@Param("email") String email);

    void deleteByEmail(@Param("email") String email);

    // ==================== 🔹 RATE LIMITING ====================

    @Query("SELECT COUNT(o) FROM Otp o WHERE o.email = :email AND o.createdAt >= :since")
    long countByEmailAndCreatedAtAfter(@Param("email") String email, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(o) > 0 FROM Otp o WHERE o.email = :email AND o.used = false AND o.expiresAt > :now")
    boolean existsByEmailAndUnusedAndNotExpired(@Param("email") String email, @Param("now") LocalDateTime now);

    // ==================== 🔹 CLEANUP QUERIES ====================

    @Modifying
    @Transactional
    @Query("DELETE FROM Otp o WHERE o.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM Otp o WHERE o.used = true AND o.createdAt < :before")
    int deleteUsedOtpsOlderThan(@Param("before") LocalDateTime before);

    // ==================== 🔹 ANALYTICS ====================

    @Query("SELECT COUNT(o) FROM Otp o WHERE o.email = :email")
    long countTotalOtpsSentByEmail(@Param("email") String email);

    @Query("""
            SELECT COUNT(o), SUM(CASE WHEN o.used = true THEN 1 ELSE 0 END)
            FROM Otp o WHERE o.email = :email
            """)
    Object[] getOtpStatsByEmail(@Param("email") String email);

    // ✅ FIX: JPQL doesn't support LIMIT, use Spring Data method naming instead
    Optional<Otp> findFirstByEmailOrderByCreatedAtDesc(@Param("email") String email);
}