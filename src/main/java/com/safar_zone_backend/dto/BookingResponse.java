package com.safar_zone_backend.dto;

import lombok.*;

@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private String id;

    private String bookingCode;

    private String packageName;

    private String travelerName;

    private String travelerEmail;

    private String travelerId;

    private String driverId;

    private Integer seats;

    private Double totalAmount;

    private String paymentStatus;

    private String bookingStatus;

    private String tripDate;

    private String tripTime;

    private String fromLocation;

    private String toLocation;

    private String imageUrl;

    private String driverName;

    private String driverPhone;

}