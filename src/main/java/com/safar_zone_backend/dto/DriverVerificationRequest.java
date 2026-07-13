package com.safar_zone_backend.dto;

import lombok.Data;

@Data
public class DriverVerificationRequest {

    private String fullName;

    private String licenseNumber;

    private String licenseImage;

    private String idProofImage;

    private String selfieImage;
}
