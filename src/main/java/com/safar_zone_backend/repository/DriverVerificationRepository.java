package com.safar_zone_backend.repository;

import com.safar_zone_backend.entity.DriverVerification;
import com.safar_zone_backend.entity.DriverVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverVerificationRepository
        extends JpaRepository<DriverVerification, String> {

    // Driver ki verification request
    Optional<DriverVerification> findByDriverId(String driverId);

    // Pending requests admin dashboard ke liye
    List<DriverVerification> findByStatus(
            DriverVerificationStatus status
    );


    // Check if driver already submitted request
    boolean existsByDriverId(String driverId);

}