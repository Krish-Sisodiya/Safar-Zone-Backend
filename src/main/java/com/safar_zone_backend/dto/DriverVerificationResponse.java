package com.safar_zone_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DriverVerificationResponse {

    private String id;

    private String driverId;

    private String fullName;

    private String email;

    private String phone;

    private String licenseNumber;

    private String licenseImage;

    private String idProofImage;

    private String selfieImage;

    private String status;
}
