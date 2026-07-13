package com.safar_zone_backend.repository;

import com.safar_zone_backend.dto.VehicleCountByType;  // ✅ Top-level DTO
import com.safar_zone_backend.entity.Vehicle;
import com.safar_zone_backend.entity.VehicleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ✅ VehicleRepository - PRODUCTION READY & OPTIMIZED
 *
 * 🔹 Features:
 * - Driver-scoped queries (JWT userId based) ✅
 * - Search + Filter + Pagination support ✅
 * - Admin moderation queries ✅
 * - Analytics & stats queries ✅
 * - Bulk operations with @Modifying ✅
 * - Proper @EntityGraph for eager loading ✅
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String>, JpaSpecificationExecutor<Vehicle> {

    // ==================== 🔹 DRIVER-SCOPED BASIC QUERIES ====================

    /**
     * ✅ Get paginated vehicles for a driver (with eager driver loading)
     */
    @EntityGraph(attributePaths = {"driver"})
    Page<Vehicle> findByDriverId(@Param("driverId") String driverId, Pageable pageable);

    /**
     * ✅ Get all vehicles for a driver (unpaginated - use for small datasets)
     */
    @EntityGraph(attributePaths = {"driver"})
    @Query("SELECT v FROM Vehicle v WHERE v.driver.id = :driverId ORDER BY v.createdAt DESC")
    List<Vehicle> findByDriverIdOrderByCreatedAtDesc(@Param("driverId") String driverId);

    /**
     * ✅ Check if vehicle number already exists for this driver (case-insensitive)
     */
    // ✅ SAHI: Explicit JPQL with correct relationship traversal
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM Vehicle v " +
            "WHERE v.driver.id = :driverId AND UPPER(v.vehicleNumber) = UPPER(:vehicleNumber)")
    boolean existsByDriverIdAndVehicleNumberIgnoreCase(
            @Param("driverId") String driverId,
            @Param("vehicleNumber") String vehicleNumber);

    /**
     * ✅ Check if vehicle number exists globally (for admin validation)
     */
    boolean existsByVehicleNumberIgnoreCase(@Param("vehicleNumber") String vehicleNumber);

    /**
     * ✅ Get single vehicle by ID + driver ID (ownership check)
     */
    // VehicleRepository.java

// ✅ FIXED: Explicit JPQL with correct relationship traversal + fixed param name
    @EntityGraph(attributePaths = {"driver"})
    @Query("SELECT v FROM Vehicle v WHERE v.id = :id AND v.driver.id = :driverId")
    Optional<Vehicle> findByIdAndDriverId(
            @Param("id") String id,
            @Param("driverId") String driverId  // ✅ Fixed: driverID → driverId (lowercase 'd')
    );

    /**
     * ✅ Count vehicles for a driver (active + inactive)
     */
    // ✅ Option A: JPQL Fix (Recommended)
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.driver.id = :driverId")
    long countByDriverId(@Param("driverId") String driverId);

    /**
     * ✅ Count including soft-deleted (native query for flexibility)
     */
    @Query(value = "SELECT COUNT(*) FROM vehicles WHERE driver_id = :driverId", nativeQuery = true)
    long countAllByDriverIdIncludingInactive(@Param("driverId") String driverId);

    // ==================== 🔹 VERIFICATION STATUS FILTERS ====================

    /**
     * ✅ Get verified vehicles for driver (List - for exports/small lists)
     */
    @EntityGraph(attributePaths = {"driver"})
    @Query("SELECT v FROM Vehicle v WHERE v.driver.id = :driverId AND v.isVerified = true ORDER BY v.createdAt DESC")
    List<Vehicle> findVerifiedByDriverId(@Param("driverId") String driverId);

    /**
     * ✅ Get verified vehicles for driver (Page - for UI pagination)
     */
    @EntityGraph(attributePaths = {"driver"})
    @Query("SELECT v FROM Vehicle v WHERE v.driver.id = :driverId AND v.isVerified = true ORDER BY v.createdAt DESC")
    Page<Vehicle> findVerifiedByDriverIdPageable(
            @Param("driverId") String driverId,
            Pageable pageable);

    /**
     * ✅ Get pending (unverified) vehicles for driver
     */
    @EntityGraph(attributePaths = {"driver"})
    @Query("SELECT v FROM Vehicle v WHERE v.driver.id = :driverId AND v.isVerified = false ORDER BY v.createdAt ASC")
    List<Vehicle> findPendingByDriverId(@Param("driverId") String driverId);

    /**
     * ✅ Get inactive vehicles for driver
     */
    @EntityGraph(attributePaths = {"driver"})
    @Query("SELECT v FROM Vehicle v WHERE v.driver.id = :driverId AND v.isActive = false ORDER BY v.createdAt DESC")
    List<Vehicle> findInactiveByDriverId(@Param("driverId") String driverId);

    // ==================== 🔹 SEARCH + FILTER QUERIES (With Pagination) ====================

    /**
     * ✅ Advanced search with multiple filters + pagination
     * Use for: Driver vehicle list with search/filter UI
     */
    // ✅ Ensure this method exists with EXACT signature:
    // ✅ Add @EntityGraph to fetch driver in SAME query
    @EntityGraph(attributePaths = {"driver"})
    @Query(
            value = "SELECT v FROM Vehicle v " +
                    "WHERE v.driver.id = :driverId " +
                    "AND (:search IS NULL OR LOWER(v.vehicleNumber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:type IS NULL OR v.type = :type) " +
                    "AND (:verified IS NULL OR v.isVerified = :verified)",
            countQuery = "SELECT COUNT(v) FROM Vehicle v " +
                    "WHERE v.driver.id = :driverId " +
                    "AND (:search IS NULL OR LOWER(v.vehicleNumber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:type IS NULL OR v.type = :type) " +
                    "AND (:verified IS NULL OR v.isVerified = :verified)"
    )
    Page<Vehicle> searchByDriverId(
            @Param("driverId") String driverId,
            @Param("search") String search,
            @Param("type") VehicleType type,
            @Param("verified") Boolean verified,
            Pageable pageable
    );


    /**
     * ✅ Simple search by vehicle number for driver (unpaginated)
     */
    @EntityGraph(attributePaths = {"driver"})
    @Query("SELECT v FROM Vehicle v WHERE v.driver.id = :driverId AND LOWER(v.vehicleNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Vehicle> searchByVehicleNumberForDriver(
            @Param("driverId") String driverId,
            @Param("keyword") String keyword);

    /**
     * ✅ Filter by type + verified status (for dashboard stats)
     */
    @EntityGraph(attributePaths = {"driver"})
    @Query("SELECT v FROM Vehicle v WHERE v.driver.id = :driverId AND v.type = :type AND v.isVerified = :verified")
    List<Vehicle> findByDriverIdAndTypeAndVerified(
            @Param("driverId") String driverId,
            @Param("type") VehicleType type,
            @Param("verified") Boolean verified);

    // ==================== 🔹 ADMIN/MODERATION QUERIES ====================

    /**
     * ✅ Get all pending vehicles (for admin approval queue)
     */
    @Query(value = """
            SELECT v.* FROM vehicles v 
            WHERE v.is_verified = false 
            ORDER BY v.created_at ASC
            """, nativeQuery = true)
    List<Vehicle> findAllPendingForModeration();

    /**
     * ✅ Admin: Search all vehicles with advanced filters
     */
    @Query("""
            SELECT v FROM Vehicle v 
            LEFT JOIN v.driver d
            WHERE (:vehicleNumber IS NULL OR LOWER(v.vehicleNumber) LIKE LOWER(CONCAT('%', :vehicleNumber, '%')))
            AND (:type IS NULL OR v.type = :type)
            AND (:verified IS NULL OR v.isVerified = :verified)
            AND (:driverEmail IS NULL OR LOWER(d.email) LIKE LOWER(CONCAT('%', :driverEmail, '%')))
            ORDER BY v.createdAt DESC
            """)
    Page<Vehicle> searchAllVehicles(
            @Param("vehicleNumber") String vehicleNumber,
            @Param("type") VehicleType type,
            @Param("verified") Boolean verified,
            @Param("driverEmail") String driverEmail,
            Pageable pageable);

    /**
     * ✅ Get verified vehicles by type (for public listings)
     */
    @EntityGraph(attributePaths = {"driver"})
    @Query("SELECT v FROM Vehicle v WHERE v.type = :type AND v.isVerified = true AND v.isActive = true")
    List<Vehicle> findPublicVehiclesByType(@Param("type") VehicleType type);

    // ==================== 🔹 BULK OPERATIONS (@Modifying) ====================

    /**
     * ✅ Bulk verify vehicles (admin action)
     */
    @Modifying
    @Transactional
    @Query("UPDATE Vehicle v SET v.isVerified = true, v.updatedAt = CURRENT_TIMESTAMP WHERE v.id IN :vehicleIds")
    int bulkMarkAsVerified(@Param("vehicleIds") List<String> vehicleIds);

    /**
     * ✅ Deactivate single vehicle (soft delete)
     * ✅ FIXED: isActive field name match
     */
    @Modifying
    @Transactional
    @Query("UPDATE Vehicle v SET v.isActive = false, v.updatedAt = CURRENT_TIMESTAMP WHERE v.id = :id AND v.driver.id = :driverId")
    int deactivateByIdAndDriverId(
            @Param("id") String id,
            @Param("driverId") String driverId);

    /**
     * ✅ Deactivate all vehicles for a driver (soft delete all)
     */
    @Modifying
    @Transactional
    @Query("UPDATE Vehicle v SET v.isActive = false, v.updatedAt = CURRENT_TIMESTAMP WHERE v.driver.id = :driverId")
    int deactivateAllByDriverId(@Param("driverId") String driverId);

    /**
     * ✅ Reactivate single vehicle
     */
    @Modifying
    @Transactional
    @Query("UPDATE Vehicle v SET v.isActive = true, v.updatedAt = CURRENT_TIMESTAMP WHERE v.id = :id AND v.driver.id = :driverId")
    int reactivateByIdAndDriverId(
            @Param("id") String id,
            @Param("driverId") String driverId);

    /**
     * ✅ Hard delete single vehicle (IRREVERSIBLE - use with caution)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Vehicle v WHERE v.id = :id AND v.driver.id = :driverId")
    int hardDeleteByIdAndDriverId(
            @Param("id") String id,
            @Param("driverId") String driverId);

    /**
     * ✅ Hard delete all vehicles for driver (IRREVERSIBLE)
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM vehicles WHERE driver_id = :driverId", nativeQuery = true)
    void hardDeleteAllByDriverId(@Param("driverId") String driverId);

    // ==================== 🔹 ANALYTICS & STATS QUERIES ====================

    /**
     * ✅ Count vehicles by type for driver (for dashboard pie chart)
     * Uses top-level DTO: VehicleCountByType
     */
    @Query("""
            SELECT new com.safar_zone_backend.dto.VehicleCountByType(v.type, COUNT(v)) 
            FROM Vehicle v 
            WHERE v.driver.id = :driverId 
            GROUP BY v.type
            """)
    List<VehicleCountByType> countVehiclesByTypeForDriver(@Param("driverId") String driverId);

    /**
     * ✅ Get status summary: [verifiedCount, pendingCount, inactiveCount]
     * Returns Object[] - cast in service layer
     */
    @Query("""
            SELECT 
                COUNT(CASE WHEN v.isVerified = true AND v.isActive = true THEN 1 END),
                COUNT(CASE WHEN v.isVerified = false AND v.isActive = true THEN 1 END),
                COUNT(CASE WHEN v.isActive = false THEN 1 END)
            FROM Vehicle v 
            WHERE v.driver.id = :driverId
            """)
    Object[] getVehicleStatusSummaryForDriver(@Param("driverId") String driverId);

    /**
     * ✅ Global stats: Total active verified vehicles (for platform stats)
     */
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.isVerified = true AND v.isActive = true")
    long countAllActiveVerifiedVehicles();

    /**
     * ✅ Get recent active verified vehicles (for homepage showcase)
     */
    @EntityGraph(attributePaths = {"driver"})
    @Query("SELECT v FROM Vehicle v WHERE v.isVerified = true AND v.isActive = true ORDER BY v.createdAt DESC")
    List<Vehicle> findRecentActiveVerifiedVehicles(Pageable pageable);

    /**
     * ✅ Count pending vehicles globally (for admin dashboard)
     */
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.isVerified = false")
    long countPendingVehicles();

    // ==================== 🔹 UTILITY METHODS ====================

    /**
     * ✅ Check if driver has any verified vehicles (for conditional UI)
     */
    @Query("SELECT COUNT(v) > 0 FROM Vehicle v WHERE v.driver.id = :driverId AND v.isVerified = true AND v.isActive = true")
    boolean driverHasVerifiedVehicle(@Param("driverId") String driverId);

    /**
     * ✅ Get vehicle image URLs for a driver (for cache preloading)
     */
    @Query("SELECT v.imageUrl FROM Vehicle v WHERE v.driver.id = :driverId AND v.imageUrl IS NOT NULL")
    List<String> findImageUrlsByDriverId(@Param("driverId") String driverId);

    /**
     * ✅ Cleanup: Find vehicles with broken image URLs (for maintenance)
     */
    @Query("SELECT v.id FROM Vehicle v WHERE v.imageUrl LIKE '%broken%' OR v.imageUrl LIKE '%error%'")
    List<String> findVehiclesWithBrokenImages();

    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.driver.id = :driverId AND v.isActive = :active")
    long countByDriverIdAndIsActive(@Param("driverId") String driverId, @Param("active") Boolean active);

    // Or for paginated results:
    @Query("SELECT v FROM Vehicle v WHERE v.driver.id = :driverId AND v.isActive = :active ORDER BY v.createdAt DESC")
    Page<Vehicle> findByDriverIdAndIsActive(
            @Param("driverId") String driverId,
            @Param("active") Boolean active,
            Pageable pageable);



    // ✅ Add this method for admin pending vehicles list
    @Query("SELECT v FROM Vehicle v WHERE v.isVerified = false AND v.isActive = true ORDER BY v.createdAt DESC")
    List<Vehicle> findPendingVehicles();

    // ✅ Optional: Paginated version for large datasets
    @Query("SELECT v FROM Vehicle v WHERE v.isVerified = false AND v.isActive = true ORDER BY v.createdAt DESC")
    Page<Vehicle> findPendingVehicles(Pageable pageable);


    // 📁 src/main/java/com/safar_zone_backend/repository/VehicleRepository.java

    // ✅ Add these simple count methods (Spring Data auto-generates the query)
    long countByDriverIdAndIsVerified(String driverId, Boolean verified);




    @Query("SELECT v FROM Vehicle v WHERE v.driver.id = :driverId " +
            "AND v.isActive = true AND v.isVerified = true " +
            "ORDER BY v.createdAt DESC")
    List<Vehicle> findByDriverIdAndIsActiveAndIsVerifiedOrderByCreatedAtDesc(
            @Param("driverId") String driverId,
            @Param("isActive") Boolean isActive,
            @Param("isVerified") Boolean isVerified);

    // ✅ Also add this for simple active vehicles (if needed)
    @Query("SELECT v FROM Vehicle v WHERE v.driver.id = :driverId AND v.isActive = true ORDER BY v.createdAt DESC")
    List<Vehicle> findByDriverIdAndIsActiveOrderByCreatedAtDesc(
            @Param("driverId") String driverId,
            @Param("isActive") Boolean isActive);


}