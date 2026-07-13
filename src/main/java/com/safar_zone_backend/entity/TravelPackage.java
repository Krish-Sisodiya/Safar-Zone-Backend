package com.safar_zone_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "packages", indexes = {
        @Index(name = "idx_driver_id", columnList = "driver_id"),  // ✅ Optimize queries
        @Index(name = "idx_driver_date", columnList = "driver_id, trip_date")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TravelPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // ✅ Store userId directly (not a @ManyToOne if you're using userId-based auth)
    @Column(name = "driver_id", nullable = false, updatable = false)
    private String driverId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String fromLocation;

    @Column(nullable = false)
    private String toLocation;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private String priceCategory; // per-seat, per-trip, etc.

    @Column(nullable = false)
    private int totalSeats;

    private int bookedSeats = 0;

    @Column(nullable = false)
    private LocalDate tripDate;

    @Column(nullable = false)
    private LocalTime tripTime;

    private String imageUrl;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private PackageStatus status = PackageStatus.UPCOMING;

    private LocalDateTime createdAt = LocalDateTime.now();

    private boolean deleted = false;

    private LocalDateTime completedAt;

    private LocalDateTime deletedAt;

    public enum PackageStatus {
        UPCOMING,
        ONGOING,
        COMPLETED,
        FULL,
        CANCELLED
    }


    private String driverName;

    private String driverImage;

    private String driverPhone;


    private int views;

    private int favourites;

    private double rating;

    private int totalBookings;
}