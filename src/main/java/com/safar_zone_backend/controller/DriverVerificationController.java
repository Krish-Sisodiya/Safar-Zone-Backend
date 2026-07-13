package com.safar_zone_backend.controller;

import com.safar_zone_backend.dto.DriverVerificationRequest;
import com.safar_zone_backend.entity.DriverVerification;
import com.safar_zone_backend.services.DriverVerificationService;
import com.safar_zone_backend.util.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
public class DriverVerificationController {

    private final DriverVerificationService service;

    // =====================================================
    // SUBMIT VERIFICATION
    // =====================================================

    @PostMapping("/verification")
    public ResponseEntity<?> submit(
            @AuthenticationPrincipal
            CustomUserDetails user,

            @RequestBody
            DriverVerificationRequest request
    ) {

        service.submitVerification(
                user.getUserId(),
                request
        );

        return ResponseEntity.ok(
                "Verification Submitted Successfully"
        );
    }

    // =====================================================
    // GET MY VERIFICATION
    // =====================================================

    @GetMapping("/verification/me")
    public ResponseEntity<?> getMyVerification(
            @AuthenticationPrincipal
            CustomUserDetails user
    ) {

        return ResponseEntity.ok(
                service.getDriverVerification(
                        user.getUserId()
                )
        );
    }

    // =====================================================
    // GET MY STATUS
    // =====================================================

    @GetMapping("/verification/status")
    public ResponseEntity<?> getMyStatus(
            @AuthenticationPrincipal
            CustomUserDetails user
    ) {

        return ResponseEntity.ok(
                service.getVerificationStatus(
                        user.getUserId()
                )
        );
    }
}