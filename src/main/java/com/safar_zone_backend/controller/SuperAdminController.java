package com.safar_zone_backend.controller;

import com.safar_zone_backend.dto.VehicleResponse;
import com.safar_zone_backend.services.DriverVerificationService;
import com.safar_zone_backend.services.VehicleService;
import com.safar_zone_backend.util.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
//@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000,exp://*,https://*.ngrok-free.dev}")
public class SuperAdminController {

    private final VehicleService vehicleService;
    private final DriverVerificationService driverVerificationService;

    @GetMapping("/vehicles")
    public ResponseEntity<?> getAllVehicles() {

        return ResponseEntity.ok(
                vehicleService.getAllVehicles()
        );
    }

    @PutMapping("/vehicles/{id}/verify")
    public ResponseEntity<?> verifyVehicle(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user
    ) {

        VehicleResponse response =
                vehicleService.verifyVehicle(user.getUserId(), id);

        return ResponseEntity.ok(response);
    }


    @PutMapping("/vehicles/{id}/reject")
    public ResponseEntity<?> rejectVehicle(
            @PathVariable String id
    ) {

        return ResponseEntity.ok(
                vehicleService.rejectVehicle(id)
        );
    }


    @PutMapping("/driver-verifications/{id}/verify")
    public ResponseEntity<?> verifyDriver(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user
    ) {

        return ResponseEntity.ok(
                driverVerificationService.verifyDriver(
                        user.getUserId(),
                        id
                )
        );
    }

    @PutMapping("/driver-verifications/{id}/reject")
    public ResponseEntity<?> rejectDriver(
            @PathVariable String id
    ) {

        return ResponseEntity.ok(
                driverVerificationService.rejectDriver(id)
        );
    }

    @GetMapping("/drivers/{id}")
    public ResponseEntity<?> getDriver(
            @PathVariable String id
    ) {

        return ResponseEntity.ok(

                driverVerificationService
                        .getDriverById(id)

        );
    }


    @GetMapping("/drivers/pending")
    public ResponseEntity<?> getPendingDrivers() {

        return ResponseEntity.ok(

                driverVerificationService
                        .getPendingDrivers()

        );
    }


    @GetMapping("/drivers")
    public ResponseEntity<?> getAllDrivers() {

        return ResponseEntity.ok(
                driverVerificationService
                        .getAllDrivers()
        );
    }

}
