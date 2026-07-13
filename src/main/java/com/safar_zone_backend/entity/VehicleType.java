package com.safar_zone_backend.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VehicleType {
    CAR("Car", "car-outline"),
    BIKE("Bike", "bike-outline"),
    AUTO("Auto", "car-sport-outline"),
    BUS("Bus", "bus-outline"),
    TEMPO("Tempo", "truck-outline");

    private final String displayName;
    private final String ioniconName;
}