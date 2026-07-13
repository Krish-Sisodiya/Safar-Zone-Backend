// src/main/java/com/safar_zone_backend/repository/PackageRepository.java
package com.safar_zone_backend.repository;

import com.safar_zone_backend.entity.TravelPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PackageRepository extends JpaRepository<TravelPackage, String> {

    // ✅ Primary query: Get packages by driver userId
    List<TravelPackage> findByDriverIdOrderByTripDateDesc(String driverId);

    List<TravelPackage> findByDriverIdOrderByTripDateDescTripTimeDesc(
            String driverId
    );

    // ✅ Critical: Fetch with ownership verification
    Optional<TravelPackage> findByIdAndDriverId(String id, String driverId);

    // ✅ Check existence before update/delete
    boolean existsByIdAndDriverId(String id, String driverId);

    // ✅ Count active packages for driver
    long countByDriverIdAndStatus(String driverId, TravelPackage.PackageStatus status);

    // ✅ Advanced: Search packages by route for a driver
    @Query("""
SELECT p
FROM TravelPackage p
WHERE p.driverId = :driverId
AND (
      LOWER(p.fromLocation) LIKE LOWER(CONCAT('%', :keyword, '%'))
      OR
      LOWER(p.toLocation) LIKE LOWER(CONCAT('%', :keyword, '%'))
)
ORDER BY p.tripDate DESC
""")
    List<TravelPackage> searchByRoute(@Param("driverId") String driverId,
                                      @Param("keyword") String keyword);




    List<TravelPackage> findByDeletedFalse();


    List<TravelPackage> findByDriverIdAndDeletedFalseOrderByTripDateDesc(
            String driverId
    );

    List<TravelPackage> findByStatusAndDeletedFalse(
            TravelPackage.PackageStatus status
    );

    List<TravelPackage> findByStatusAndDeletedFalseAndCompletedAtBefore(
            TravelPackage.PackageStatus status,
            LocalDateTime dateTime
    );
}
