package com.safar_zone_backend.repository;

import com.safar_zone_backend.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository
        extends JpaRepository<Booking, String> {

    Optional<Booking> findByBookingCode(
            String bookingCode
    );

    List<Booking> findByTravelerIdOrderByCreatedAtDesc(
            String travelerId
    );

    List<Booking> findByDriverIdOrderByCreatedAtDesc(
            String driverId
    );
}