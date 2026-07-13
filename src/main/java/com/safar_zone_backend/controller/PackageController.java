// src/main/java/com/safar_zone_backend/controller/PackageController.java
package com.safar_zone_backend.controller;

import com.safar_zone_backend.dto.ApiResponse;
import com.safar_zone_backend.dto.CreatePackageRequest;
import com.safar_zone_backend.dto.PackageResponse;
import com.safar_zone_backend.dto.UpdatePackageRequest;
import com.safar_zone_backend.services.PackageService;
import com.safar_zone_backend.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/driver/packages")
@RequiredArgsConstructor
@Tag(name = "Driver Packages", description = "Package management for authenticated drivers")
public class PackageController {

    private final PackageService packageService;
    private final SecurityUtil securityUtil;  // ✅ For logging/auditing

    @PostMapping
    @Operation(summary = "Create new travel package")
    public ResponseEntity<ApiResponse<PackageResponse>> create(@Valid @RequestBody CreatePackageRequest req) {
        log.info("📥 POST /packages - Creating package for driver: {}", securityUtil.getCurrentUserId());

        PackageResponse response = packageService.create(req);
        return ResponseEntity.ok(ApiResponse.success(response, "Package created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all packages for authenticated driver")
    public ResponseEntity<ApiResponse<List<PackageResponse>>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "tripDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        log.debug("📥 GET /packages - Fetching for driver: {}", securityUtil.getCurrentUserId());

        List<PackageResponse> packages =
                packageService.getPackages(
                        keyword,
                        status,
                        sortBy,
                        sortDir
                );
        return ResponseEntity.ok(ApiResponse.success(packages, "Packages fetched successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get package by ID (ownership verified)")
    public ResponseEntity<ApiResponse<PackageResponse>> getById(@PathVariable String id) {
        log.debug("📥 GET /packages/{} - Fetching for driver: {}", id, securityUtil.getCurrentUserId());

        PackageResponse pkg = packageService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(pkg, "Package fetched successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete package (ownership verified)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        log.info("📥 DELETE /packages/{} - Deleting by driver: {}", id, securityUtil.getCurrentUserId());

        packageService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Package deleted successfully"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<PackageResponse>>
    updatePackage(

            @PathVariable String id,

            @RequestBody
            UpdatePackageRequest request
    ){

        return ResponseEntity.ok(
                ApiResponse.success(
                        packageService.updatePackage(
                                id,
                                request
                        ),
                        "Package updated"
                )
        );
    }
}