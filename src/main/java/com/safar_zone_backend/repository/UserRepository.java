package com.safar_zone_backend.repository;

import com.safar_zone_backend.entity.Role;
import com.safar_zone_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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
public interface UserRepository extends JpaRepository<User, String> {

    // ==================== 🔹 EMAIL QUERIES ====================

    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmailWithRelations(@Param("email") String email);

    // ==================== 🔹 PHONE QUERIES ====================

    @Query("SELECT u FROM User u WHERE REPLACE(REPLACE(REPLACE(u.phone, ' ', ''), '-', ''), '+', '') = :normalizedPhone")
    Optional<User> findByPhone(@Param("normalizedPhone") String normalizedPhone);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE REPLACE(REPLACE(REPLACE(u.phone, ' ', ''), '-', ''), '+', '') = :normalizedPhone")
    boolean existsByPhone(@Param("normalizedPhone") String normalizedPhone);

    // ==================== 🔹 ROLE-BASED QUERIES ====================

    @Query("SELECT u FROM User u WHERE u.role = :role ORDER BY u.createdAt DESC")
    Page<User> findByRole(@Param("role") Role role, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") Role role);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isVerified = true ORDER BY u.name ASC")
    List<User> findVerifiedByRole(@Param("role") Role role);

    // ==================== 🔹 SEARCH QUERIES ====================

    @Query("""
            SELECT u FROM User u 
            WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR REPLACE(REPLACE(REPLACE(u.phone, ' ', ''), '-', ''), '+', '') LIKE CONCAT('%', :keyword, '%')
            ORDER BY u.createdAt DESC
            """)
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT u FROM User u 
            WHERE (:role IS NULL OR u.role = :role)
            AND (
                LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            ORDER BY u.createdAt DESC
            """)
    Page<User> searchUsersByRoleAndKeyword(
            @Param("role") Role role,
            @Param("keyword") String keyword,
            Pageable pageable);

    // ==================== 🔹 ANALYTICS ====================

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    long countUsersRegisteredBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT u.role, u.isVerified, COUNT(u) 
            FROM User u 
            GROUP BY u.role, u.isVerified
            """)
    List<Object[]> countUsersByRoleAndVerification();

    // ==================== 🔹 BULK OPERATIONS ====================

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isVerified = true, u.updatedAt = CURRENT_TIMESTAMP WHERE LOWER(u.email) IN :emails")
    int bulkVerifyUsers(@Param("emails") List<String> emails);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.role = :newRole, u.updatedAt = CURRENT_TIMESTAMP WHERE LOWER(u.email) IN :emails")
    int bulkUpdateRole(@Param("emails") List<String> emails, @Param("newRole") Role newRole);

    // ✅ FIX #1: isActive → isVerified (match User entity field)
    // ✅ FIX #2: CURRENT_TIMESTAMP() → CURRENT_TIMESTAMP (JPQL syntax)
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isVerified = false, u.updatedAt = CURRENT_TIMESTAMP WHERE LOWER(u.email) = :email")
    int deactivateUserByEmail(@Param("email") String email);

    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.id = :userId")
    int hardDeleteUserById(@Param("userId") String userId);

    // ==================== 🔹 UTILITY ====================

    @Query("""
            SELECT u FROM User u 
            WHERE u.isVerified = false AND u.createdAt < :cutoffDate
            ORDER BY u.createdAt ASC
            """)
    List<User> findUnverifiedUsersOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT u.email FROM User u WHERE u.id = :userId")
    Optional<String> findEmailByUserId(@Param("userId") String userId);

    // ==================== 🔹 ADD THESE SIMPLE METHODS (Spring Data Magic) ====================

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);


    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email) AND u.isVerified = true")
    Optional<User> findVerifiedByEmail(@Param("email") String email);
}