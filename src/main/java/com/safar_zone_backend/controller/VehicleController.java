package com.safar_zone_backend.controller;

import com.safar_zone_backend.dto.ApiResponse;
import com.safar_zone_backend.dto.CreateVehicleRequest;
import com.safar_zone_backend.dto.PageResponse;
import com.safar_zone_backend.dto.VehicleResponse;
import com.safar_zone_backend.entity.VehicleType;
import com.safar_zone_backend.services.FileStorageService;
import com.safar_zone_backend.services.VehicleService;
import com.safar_zone_backend.util.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/driver")  // ✅ Base path: /api/driver
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000,exp://*,https://*.ngrok-free.dev}")
public class VehicleController {

    private final VehicleService vehicleService;
    private final FileStorageService fileStorageService;

    // ==================== 🔹 CREATE ====================

    @PostMapping("/vehicles")
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CreateVehicleRequest req) {

        log.info("📥 POST /vehicles - User: {}", userDetails.getUserId());

        try {
            VehicleResponse vehicle = vehicleService.createByUserId(userDetails.getUserId(), req);
            log.info("✅ Vehicle created: {} (id: {})", vehicle.getVehicleNumber(), vehicle.getId());

            return ResponseEntity.status(201)
                    .body(ApiResponse.success(vehicle, "Vehicle added successfully"));
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Create vehicle failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("❌ Create vehicle error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to add vehicle. Please try again.", 500));
        }
    }

    // ==================== 🔹 READ - LIST WITH PAGINATION + SEARCH + FILTER ✅ FIXED ====================

    @GetMapping("/vehicles")
    public ResponseEntity<ApiResponse<PageResponse<VehicleResponse>>> getVehicles(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) VehicleType type,
            @RequestParam(required = false) Boolean verified) {

        log.debug("📥 GET /vehicles - User: {}, page: {}, size: {}, filters: search={}, type={}, verified={}",
                userDetails.getUserId(), page, size, search, type, verified);

        try {
            // ✅ Pageable banana, lekin service ko alag params dena
            Sort sort = (sortBy != null && !sortBy.isBlank())
                    ? Sort.by(Sort.Direction.fromString(sortDir != null ? sortDir : "desc"), sortBy)
                    : Sort.by(Sort.Direction.DESC, "createdAt");

            Pageable pageable = PageRequest.of(page, size, sort);

            // ✅ Service call - individual params pass karo (existing signature ke hisaab se)
            Page<VehicleResponse> vehicles = vehicleService.getVehiclesByDriverId(
                    userDetails.getUserId(),
                    pageable.getPageNumber(),      // ✅ page
                    pageable.getPageSize(),        // ✅ size
                    pageable.getSort().toString().isEmpty() ? "createdAt" : pageable.getSort().get().findFirst().get().getProperty(), // ✅ sortBy
                    pageable.getSort().toString().isEmpty() ? "desc" : pageable.getSort().get().findFirst().get().getDirection().name().toLowerCase(), // ✅ sortDir
                    search,
                    type,
                    verified
            );

            log.debug("✅ Fetched {} vehicles (page {}/{})",
                    vehicles.getNumberOfElements(), vehicles.getNumber(), vehicles.getTotalPages());

            // ✅ Convert to stable PageResponse
            PageResponse<VehicleResponse> stablePage = PageResponse.fromPage(vehicles);

            return ResponseEntity.ok(ApiResponse.success(stablePage, "Vehicles fetched successfully"));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid sort params: sortBy={}, sortDir={}", sortBy, sortDir);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid sort parameters. Use: createdAt/totalSeats, asc/desc", 400));
        } catch (Exception e) {
            log.error("❌ Fetch vehicles error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch vehicles. Please try again.", 500));
        }
    }


    // 📁 src/main/java/com/safar_zone_backend/controller/VehicleController.java

    /**
     * ✅ NEW: Simple vehicle list for CreatePackageScreen dropdown
     * GET /api/driver/vehicles/available
     * Returns: List<VehicleResponse> (no pagination wrapper)
     */
    @GetMapping("/vehicles/available")
    @Operation(summary = "Get available vehicles for package creation (simple list)")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getAvailableVehicles(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.debug("📥 GET /vehicles/available - User: {}", userDetails.getUserId());

        try {
            // ✅ Get simple list - active + verified only
            List<VehicleResponse> vehicles = vehicleService
                    .getAvailableVehiclesForPackages(userDetails.getUserId());

            log.debug("✅ Found {} available vehicles", vehicles.size());

            return ResponseEntity.ok(
                    ApiResponse.success(vehicles, "Available vehicles fetched")
            );

        } catch (Exception e) {
            log.error("❌ Fetch available vehicles error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch vehicles. Please try again.", 500));
        }
    }

    // ==================== 🔹 READ - SINGLE VEHICLE ====================

    @GetMapping("/vehicles/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicle(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String id) {

        log.debug("🔍 GET /vehicles/{} - User: {}", id, userDetails.getUserId());

        try {
            VehicleResponse vehicle = vehicleService.getById(userDetails.getUserId(), id);
            return ResponseEntity.ok(ApiResponse.success(vehicle, "Vehicle fetched"));
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Vehicle not found or access denied: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Vehicle not found", 404));
        } catch (Exception e) {
            log.error("❌ Fetch vehicle error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch vehicle. Please try again.", 500));
        }
    }

    // ==================== 🔹 STATS ENDPOINT ====================

    @GetMapping("/vehicles/stats")
    public ResponseEntity<ApiResponse<VehicleStatsResponse>> getVehicleStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.debug("📊 GET /vehicles/stats - User: {}", userDetails.getUserId());

        try {
            var stats = vehicleService.getStatsByUserId(userDetails.getUserId());

            var response = new VehicleStatsResponse(
                    stats.verifiedCount(),
                    stats.pendingCount(),
                    stats.inactiveCount(),
                    stats.countsByType()
            );
            return ResponseEntity.ok(ApiResponse.success(response, "Stats fetched"));
        } catch (Exception e) {
            log.error("❌ Fetch stats error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch stats. Please try again.", 500));
        }
    }

    // ==================== 🔹 UPDATE ====================

    @PutMapping("/vehicles/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicle(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String id,
            @RequestBody @Valid CreateVehicleRequest req) {

        log.info("✏️ PUT /vehicles/{} - User: {}", id, userDetails.getUserId());

        try {
            VehicleResponse updated = vehicleService.update(userDetails.getUserId(), id, req);
            return ResponseEntity.ok(ApiResponse.success(updated, "Vehicle updated successfully"));
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Update failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("❌ Update error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to update vehicle. Please try again.", 500));
        }
    }

    // ==================== 🔹 IMAGE UPLOAD ====================

    @PostMapping(value = "/vehicles/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VehicleResponse>> uploadVehicleImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String id,
            @RequestParam("file") MultipartFile file) {

        log.info("🖼️ POST /vehicles/{}/image - User: {}, file: {} ({})",
                id, userDetails.getUserId(), file.getOriginalFilename(), file.getSize());

        // ✅ Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Image file is required", 400));
        }
        if (!file.getContentType().startsWith("image/")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Please upload a valid image file (JPEG/PNG/WebP)", 400));
        }
        if (file.getSize() > 5 * 1024 * 1024) {  // 5MB limit
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Image size should be less than 5MB", 400));
        }

        try {
            // ✅ Service handles upload + update + returns full VehicleResponse
            VehicleResponse updated = vehicleService.updateImage(userDetails.getUserId(), id, file);
            return ResponseEntity.ok(ApiResponse.success(updated, "Image uploaded successfully"));
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Image upload failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("❌ Image upload error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to upload image. Please try again.", 500));
        }
    }

    // ==================== 🔹 DELETE ====================

    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String id) {

        log.info("🗑️ DELETE /vehicles/{} - User: {}", id, userDetails.getUserId());

        try {
            vehicleService.delete(userDetails.getUserId(), id);
            return ResponseEntity.ok(ApiResponse.success(null, "Vehicle removed successfully"));
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Delete failed: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Vehicle not found", 404));
        } catch (Exception e) {
            log.error("❌ Delete error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to remove vehicle. Please try again.", 500));
        }
    }

    @DeleteMapping("/vehicles/{id}/permanent")
    public ResponseEntity<ApiResponse<Void>> hardDeleteVehicle(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String id) {

        log.warn("🔥 DELETE /vehicles/{}/permanent - User: {}", id, userDetails.getUserId());

        try {
            vehicleService.hardDelete(userDetails.getUserId(), id);
            return ResponseEntity.ok(ApiResponse.success(null, "Vehicle permanently deleted"));
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Hard delete failed: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Vehicle not found", 404));
        } catch (Exception e) {
            log.error("❌ Hard delete error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to delete vehicle. Please try again.", 500));
        }
    }

    // ==================== 🔹 HELPER DTOs ====================

    public record VehicleStatsResponse(
            Long verifiedCount,
            Long pendingCount,
            Long inactiveCount,
            java.util.Map<VehicleType, Long> countsByType
    ) {}
}