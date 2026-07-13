package com.safar_zone_backend.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverBookingResponse {

    private String bookingId;

    private String packageId;

    private String packageName;

    private String travelerName;

    private String travelerEmail;

    private Integer seats;

    private Double totalAmount;

    private String bookingStatus;

    private String paymentStatus;

    private String fromLocation;

    private String toLocation;

    private String tripDate;

    private String imageUrl;
}