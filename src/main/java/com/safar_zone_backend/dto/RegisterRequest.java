package com.safar_zone_backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank private String name;
    @NotBlank @Email private String email;
    @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian phone number")
    private String phone;
    @NotBlank @Size(min = 6, max = 50) private String password;
    @NotBlank private String role;
    private String licenseNumber;
}