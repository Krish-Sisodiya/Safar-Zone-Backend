package com.safar_zone_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String driverId;

    private String fullName;

    private String licenseNumber;

    private String licenseImage;

    private String idProofImage;

    private String selfieImage;

    @Enumerated(EnumType.STRING)
    private DriverVerificationStatus status;

    private String rejectionReason;
}