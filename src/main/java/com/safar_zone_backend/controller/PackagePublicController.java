// 📁 PackagePublicController.java

package com.safar_zone_backend.controller;

import com.safar_zone_backend.dto.ApiResponse;
import com.safar_zone_backend.dto.PackageResponse;
import com.safar_zone_backend.services.PackageService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class PackagePublicController {

    private final PackageService packageService;

    // ✅ Public packages for travelers

    @GetMapping
    public ResponseEntity<ApiResponse<List<PackageResponse>>> getAllPackages() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        packageService.getAllPackages(),
                        "Packages fetched successfully"
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PackageResponse>>
    getPackage(@PathVariable String id) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        packageService.getPublicPackageById(id),
                        "Package fetched"
                )
        );
    }
}