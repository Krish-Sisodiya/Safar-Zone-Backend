package com.safar_zone_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank
    private String email;

    @NotBlank
    private String otp; // 6 digits
}
