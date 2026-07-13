package com.safar_zone_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpRequest {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String role; // TRAVELER, DRIVER, ADMIN
}
