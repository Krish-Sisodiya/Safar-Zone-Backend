package com.safar_zone_backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank @Email private String email;
    @NotBlank private String password;
    @NotBlank private String role; // TRAVELER, DRIVER, ADMIN
}
