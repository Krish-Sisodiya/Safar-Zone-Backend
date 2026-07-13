package com.safar_zone_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // =====================================================
    // BOOKING CODE
    // =====================================================

    @Column(unique = true, nullable = false)
    private String bookingCode;

    // =====================================================
    // USER IDS
    // =====================================================

    @Column(nullable = false)
    private String travelerId;

    @Column(nullable = false)
    private String driverId;

    // =====================================================
    // PACKAGE
    // =====================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id")
    private TravelPackage travelPackage;

    // =====================================================
    // BOOKING DETAILS
    // =====================================================

    private Integer seats;

    @Column(name = "total_price")
    private Double totalAmount;

    private String couponCode;

    private Double discountAmount;

    // =====================================================
    // PAYMENT
    // =====================================================

    private String paymentId;

    private String razorpayOrderId;

    private String paymentMethod;

    // =====================================================
    // STATUS
    // =====================================================

    @Enumerated(EnumType.STRING)
    private BookingStatus bookingStatus;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    // =====================================================
    // QR EXPIRY
    // =====================================================

    private LocalDateTime qrExpiryTime;

    // =====================================================
    // BOOKING VALIDITY
    // =====================================================

    private LocalDateTime validTill;

    // =====================================================
    // TIMESTAMPS
    // =====================================================

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // =====================================================
    // AUTO TIMESTAMP
    // =====================================================

    @PrePersist
    public void prePersist() {

        createdAt = LocalDateTime.now();

        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {

        updatedAt = LocalDateTime.now();
    }

    // =====================================================
    // ENUMS
    // =====================================================

    public enum BookingStatus {

        PENDING,
        CONFIRMED,
        CANCELLED,
        COMPLETED,
        EXPIRED
    }

    public enum PaymentStatus {

        PENDING,
        PAID,
        FAILED,
        REFUNDED
    }
}