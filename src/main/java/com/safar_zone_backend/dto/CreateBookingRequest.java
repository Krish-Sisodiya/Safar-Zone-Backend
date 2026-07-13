package com.safar_zone_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBookingRequest {

    @NotBlank
    private String packageId;

    @Min(1)
    private Integer seats;

    private String couponCode;
}