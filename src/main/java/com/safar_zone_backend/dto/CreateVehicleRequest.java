package com.safar_zone_backend.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateVehicleRequest {

    @NotBlank(message = "Vehicle number is required")
    @Pattern(regexp = "^[A-Z0-9\\-\\s]{6,20}$", message = "Invalid vehicle number format")
    private String vehicleNumber;

    @NotBlank(message = "Vehicle type is required")
    private String type;

    @NotNull(message = "Total seats is required")
    @Min(value = 1, message = "Seats must be at least 1")
    @Max(value = 20, message = "Seats cannot exceed 20")
    private Integer totalSeats;

    private String imageUrl;
}