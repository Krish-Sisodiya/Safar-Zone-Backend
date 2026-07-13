package com.safar_zone_backend.dto;

import lombok.Data;

@Data
public class UpdatePackageRequest {

    private String name;

    private String imageUrl;

    private String tripDate;

    private String tripTime;

    private Double price;

    private String priceCategory;

    private Integer totalSeats;

    private String description;
}
