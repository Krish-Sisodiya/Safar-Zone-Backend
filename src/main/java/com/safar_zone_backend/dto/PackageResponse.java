package com.safar_zone_backend.dto;

import com.safar_zone_backend.entity.TravelPackage;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PackageResponse {
    private String id, name, vehicleNumber, fromLocation, toLocation, description, imageUrl;
    private double price;

    private String priceCategory;

    int totalSeats, bookedSeats;

    private String tripDate, tripTime, duration, driverName;

    private String status;

    private long distance;


    private String driverId;

    private String driverImage;

    private String driverPhone;

    private int availableSeats;

    private long hoursRemaining;

    private boolean bookingAllowed;



}
