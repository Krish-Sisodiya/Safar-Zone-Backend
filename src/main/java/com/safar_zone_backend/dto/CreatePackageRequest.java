package com.safar_zone_backend.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreatePackageRequest {
    private String name, vehicleId, fromLocation, toLocation, description, imageUrl;
    private double price; private String priceCategory; private int totalSeats;
    private String tripDate; // YYYY-MM-DD
    private String tripTime; // HH:mm
}

