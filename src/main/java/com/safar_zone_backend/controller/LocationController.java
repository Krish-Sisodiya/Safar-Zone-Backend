package com.safar_zone_backend.controller;

import com.safar_zone_backend.entity.LocationRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/location")
@CrossOrigin("*")
public class LocationController {

    @PostMapping("/save")
    public String saveLocation(@RequestBody LocationRequest request) {

        System.out.println("Latitude: " + request.getLatitude());
        System.out.println("Longitude: " + request.getLongitude());

        return "Location Saved";
    }
}
