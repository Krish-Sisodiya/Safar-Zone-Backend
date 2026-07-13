package com.safar_zone_backend.services;

import com.safar_zone_backend.dto.CreateVehicleRequest;
import com.safar_zone_backend.dto.VehicleCountByType;
import com.safar_zone_backend.dto.VehicleResponse;
import com.safar_zone_backend.entity.User;
import com.safar_zone_backend.entity.Vehicle;
import com.safar_zone_backend.entity.VehicleType;
import com.safar_zone_backend.event.VehicleCreatedEvent;
import com.safar_zone_backend.repository.UserRepository;
import com.safar_zone_backend.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    // ✅ ADD THIS: Event publisher inject karo
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.vehicle.max-per-driver:5}")
    private int maxVehiclesPerDriver;

    // ==================== 🔹 CREATE ====================

    @Transactional
    public VehicleResponse createByUserId(String userId, CreateVehicleRequest req) {
        log.info("🚗 Creating vehicle for user: {}", userId);
        validateCreateRequest(req);

        User driver = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found. Please login again."));

        if (hasReachedVehicleLimit(driver)) {
            throw new IllegalArgumentException("Maximum " + maxVehiclesPerDriver + " vehicles allowed per driver");
        }

        String vehicleNumber = sanitizeVehicleNumber(req.getVehicleNumber());
        VehicleType vehicleType = validateAndParseVehicleType(req.getType());
        Integer totalSeats = validateSeatCount(req.getTotalSeats());

        if (vehicleRepository.existsByDriverIdAndVehicleNumberIgnoreCase(driver.getId(), vehicleNumber)) {
            throw new IllegalArgumentException("You have already added this vehicle");
        }

        String imageUrl = processImageInput(req.getImageUrl());

        Vehicle vehicle = Vehicle.builder()
                .driver(driver)
                .vehicleNumber(vehicleNumber)
                .type(vehicleType)
                .totalSeats(totalSeats)
                .imageUrl(imageUrl)
                .isVerified(false)
                .isActive(true)  // ✅ FIXED: Use isActive (Boolean wrapper) matching entity
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);

        log.info("✅ Vehicle saved: {} (id: {})", saved.getVehicleNumber(), saved.getId());


        // ✅ Event publish karo (async processing ke liye)
        try {
            applicationEventPublisher.publishEvent(
                    VehicleCreatedEvent.builder()
                            .vehicleId(saved.getId())
                            .driverId(saved.getDriver().getId())
                            .vehicleType(saved.getType())
                            .vehicleNumber(saved.getVehicleNumber())
                            .build()
            );
            log.debug("📤 Published VehicleCreatedEvent for: {}", saved.getId());
        } catch (Exception e) {
            // ⚠️ Non-critical: Event publish fail hone se main transaction affect nahi hona chahiye
            log.warn("⚠️ Failed to publish VehicleCreatedEvent (non-blocking): {}", e.getMessage());
            // Don't throw - vehicle creation should still succeed
        }

        return convertToResponse(saved, userId);
    }

    // ==================== 🔹 READ - UNIFIED SEARCH WITH PAGINATION ✅ ====================

    /**
     * ✅ MAIN METHOD: Get paginated vehicles for driver with search/filter support
     * This is what VehicleController.getVehicles() calls
     */
    @Transactional(readOnly = true)
    public Page<VehicleResponse> getVehiclesByDriverId(
            String driverId,
            int page,
            int size,
            String sortBy,
            String sortDir,
            String search,
            VehicleType type,
            Boolean verified
    ) {
        log.debug("🔍 Querying vehicles for driver: {}, page: {}, filters: search={}, type={}, verified={}",
                driverId, page, search, type, verified);

        // ✅ Create proper Sort object
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // ✅ Use repository's paginated search method (efficient - DB level filtering)
        Page<Vehicle> vehiclePage = vehicleRepository.searchByDriverId(
                driverId, search, type, verified, pageable);

        // ✅ Convert entities to responses
        return vehiclePage.map(vehicle -> convertToResponse(vehicle, driverId));
    }


    /**
     * ✅ Legacy method: Get all vehicles (unpaginated - use only for small datasets)
     */
    @Transactional(readOnly = true)
    @Deprecated
    public List<VehicleResponse> getAllByUserId(String userId) {
        log.warn("⚠️ Using deprecated getAllByUserId - consider using paginated version");

        List<Vehicle> vehicles = vehicleRepository.findByDriverIdOrderByCreatedAtDesc(userId);
        return vehicles.stream()
                .map(v -> convertToResponse(v, userId))
                .collect(Collectors.toList());
    }

    /**
     * ✅ Get single vehicle by ID with ownership check
     */
    @Transactional(readOnly = true)
    public VehicleResponse getById(String userId, String vehicleId) {
        log.debug("🔍 Fetching vehicle: {} for user: {}", vehicleId, userId);

        Vehicle vehicle = vehicleRepository.findByIdAndDriverId(vehicleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found or access denied"));

        return convertToResponse(vehicle, userId);
    }

    /**
     * ✅ Get verified vehicles only (for public listings)
     */
    @Transactional(readOnly = true)
    public List<VehicleResponse> getVerifiedByUserId(String userId) {
        log.debug("📦 Fetching verified vehicles for user: {}", userId);

        List<Vehicle> vehicles = vehicleRepository.findVerifiedByDriverId(userId);
        return vehicles.stream()
                .map(v -> convertToResponse(v, userId))
                .collect(Collectors.toList());
    }

    // ==================== 🔹 UPDATE ====================

    @Transactional
    public VehicleResponse update(String userId, String vehicleId, CreateVehicleRequest req) {
        log.info("✏️ Updating vehicle: {} for user: {}", vehicleId, userId);

        Vehicle vehicle = getVehicleWithOwnershipCheck(userId, vehicleId);

        if (StringUtils.hasText(req.getVehicleNumber())) {
            String newNumber = sanitizeVehicleNumber(req.getVehicleNumber());
            if (!vehicle.getVehicleNumber().equalsIgnoreCase(newNumber) &&
                    vehicleRepository.existsByDriverIdAndVehicleNumberIgnoreCase(userId, newNumber)) {
                throw new IllegalArgumentException("Another vehicle with this number already exists");
            }
            vehicle.setVehicleNumber(newNumber);
        }

        if (req.getType() != null) {
            vehicle.setType(validateAndParseVehicleType(req.getType()));
        }

        if (req.getTotalSeats() != null) {
            vehicle.setTotalSeats(validateSeatCount(req.getTotalSeats()));
        }

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("✅ Vehicle updated: {}", vehicleId);

        return convertToResponse(saved, userId);
    }

    @Transactional
    public VehicleResponse updateImage(String userId, String vehicleId, MultipartFile file) {
        log.info("🖼️ Updating image for vehicle: {} by user: {}", vehicleId, userId);

        validateImageFile(file);

        Vehicle vehicle = getVehicleWithOwnershipCheck(userId, vehicleId);
        deleteOldImageFile(vehicle.getImageUrl());

        try {
            String relativeUrl = fileStorageService.saveFile(file, "vehicles");
            vehicle.updateImageUrl(relativeUrl);
            Vehicle saved = vehicleRepository.save(vehicle);
            log.info("✅ Image updated for vehicle: {}", vehicleId);
            return convertToResponse(saved, userId);
        } catch (IOException e) {
            log.error("❌ Failed to save image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload image. Please try again.", e);
        }
    }

    @Transactional
    public VehicleResponse updateImageUrl(String userId, String vehicleId, String imageUrl) {
        log.info("🖼️ Updating image URL for vehicle: {} by user: {}", vehicleId, userId);

        Vehicle vehicle = getVehicleWithOwnershipCheck(userId, vehicleId);
        deleteOldImageFile(vehicle.getImageUrl());

        String processedUrl = processImageInput(imageUrl);
        vehicle.updateImageUrl(processedUrl);

        Vehicle saved = vehicleRepository.save(vehicle);
        return convertToResponse(saved, userId);
    }

    // ==================== 🔹 DELETE ====================

    @Transactional
    public void delete(String userId, String vehicleId) {
        log.info("🗑️ Soft deleting vehicle: {} for user: {}", vehicleId, userId);

        Vehicle vehicle = getVehicleWithOwnershipCheck(userId, vehicleId);
        deleteOldImageFile(vehicle.getImageUrl());

        vehicle.setIsActive(false);  // ✅ Soft delete: set isActive = false
        vehicleRepository.save(vehicle);

        log.info("✅ Vehicle deactivated: {} (id: {})", vehicle.getVehicleNumber(), vehicle.getId());
    }

    @Transactional
    public void hardDelete(String userId, String vehicleId) {
        log.warn("🔥 Hard deleting vehicle: {} for user: {}", vehicleId, userId);

        Vehicle vehicle = getVehicleWithOwnershipCheck(userId, vehicleId);
        deleteOldImageFile(vehicle.getImageUrl());

        vehicleRepository.delete(vehicle);
        log.info("✅ Vehicle permanently deleted: {}", vehicleId);
    }

    // ==================== 🔹 ADMIN OPERATIONS ====================

    @Transactional
    public VehicleResponse verifyVehicle(String adminUserId, String vehicleId) {
        log.info("✓ Admin verifying vehicle: {} by {}", vehicleId, adminUserId);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        vehicle.setIsVerified(true);
        Vehicle saved = vehicleRepository.save(vehicle);

        log.info("✅ Vehicle verified: {}", vehicleId);
        return convertToResponse(saved, adminUserId);
    }

    // ==================== 🔹 STATS ====================

    public record VehicleStats(
            Long verifiedCount,
            Long pendingCount,
            Long inactiveCount,
            Map<VehicleType, Long> countsByType
    ) {
        public long totalActive() {
            return (verifiedCount != null ? verifiedCount : 0L) +
                    (pendingCount != null ? pendingCount : 0L);
        }
    }

    @Transactional(readOnly = true)
    public VehicleStats getStatsByUserId(String userId) {
        log.debug("📊 Fetching vehicle stats for user: {}", userId);

        // ✅ Use repository queries for efficient counting
        // ✅ Direct count queries - no pagination, no Pageable, 100% efficient
        long verifiedCount = vehicleRepository.countByDriverIdAndIsVerified(userId, true);
        long pendingCount  = vehicleRepository.countByDriverIdAndIsVerified(userId, false);
        long inactiveCount = vehicleRepository.countByDriverIdAndIsActive(userId, false);
        // ✅ Count by type using dedicated query
        List<VehicleCountByType> typeCounts = vehicleRepository.countVehiclesByTypeForDriver(userId);

        Map<VehicleType, Long> countsByType = typeCounts.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        VehicleCountByType::getType,
                        VehicleCountByType::getCount,
                        Long::sum
                ));

        return new VehicleStats(verifiedCount, pendingCount, inactiveCount, countsByType);
    }

    // ==================== 🔹 HELPERS ====================

    private Vehicle getVehicleWithOwnershipCheck(String userId, String vehicleId) {
        return vehicleRepository.findByIdAndDriverId(vehicleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found or access denied"));
    }

    private void validateCreateRequest(CreateVehicleRequest req) {
        if (req == null) throw new IllegalArgumentException("Vehicle request cannot be null");
        if (!StringUtils.hasText(req.getVehicleNumber())) throw new IllegalArgumentException("Vehicle number is required");
        if (!StringUtils.hasText(req.getType())) throw new IllegalArgumentException("Vehicle type is required");
        if (req.getTotalSeats() == null) throw new IllegalArgumentException("Number of seats is required");
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Image file is required");
        if (!file.getContentType().startsWith("image/")) throw new IllegalArgumentException("Please upload a valid image file (JPEG/PNG/WebP)");
        if (file.getSize() > 5 * 1024 * 1024) throw new IllegalArgumentException("Image size should be less than 5MB");
    }

    private String sanitizeVehicleNumber(String input) {
        if (!StringUtils.hasText(input)) throw new IllegalArgumentException("Vehicle number is required");
        return input.trim().toUpperCase().replaceAll("\\s+", "");
    }

    private VehicleType validateAndParseVehicleType(String type) {
        if (!StringUtils.hasText(type)) throw new IllegalArgumentException("Vehicle type is required");
        try {
            return VehicleType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid vehicle type: {}", type);
            throw new IllegalArgumentException("Invalid vehicle type. Allowed values: " +
                    String.join(", ", java.util.Arrays.stream(VehicleType.values())
                            .map(VehicleType::name)
                            .toArray(String[]::new)));
        }
    }

    private Integer validateSeatCount(Integer seats) {
        if (seats == null) throw new IllegalArgumentException("Number of seats is required");
        if (seats < 1 || seats > 50) throw new IllegalArgumentException("Seats must be between 1 and 50");
        return seats;
    }

    private String processImageInput(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) return null;

        // ✅ Accept only HTTP URLs (Cloudinary/S3)
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            // Optional: Validate domain if you want to restrict to your CDN
            if (!imageUrl.contains("cloudinary.com") && !imageUrl.contains("safarzone.com")) {
                log.warn("⚠️ Image URL from untrusted domain: {}", imageUrl);
                // Don't block - allow for flexibility, but log for audit
            }
            return imageUrl;
        }

        // ❌ Reject base64 in production (log + throw)
        if (imageUrl.startsWith("data:image")) {
            log.error("❌ Base64 image upload not supported in production. Use Cloudinary upload endpoint.");
            throw new IllegalArgumentException("Image upload failed. Please try again or contact support.");
        }

        // Fallback: Return null (image optional)
        return null;
    }

    private void deleteOldImageFile(String imageUrl) {
        if (StringUtils.hasText(imageUrl) && !imageUrl.startsWith("http")) {
            try {
                fileStorageService.deleteFile(imageUrl);
                log.debug("🗑️ Deleted old image file: {}", imageUrl);
            } catch (Exception e) {
                log.warn("⚠️ Could not delete old image file: {}", imageUrl, e);
            }
        }
    }

    public String getFullImageUrl(String relativeUrl) {
        if (!StringUtils.hasText(relativeUrl)) return null;
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) return relativeUrl;
        String cleanBaseUrl = baseUrl.replaceAll("/+$", "");
        String cleanRelative = relativeUrl.replaceFirst("^/+", "");
        return cleanBaseUrl + "/" + cleanRelative;
    }

    private String formatTimestamp(LocalDateTime dt) {
        if (dt == null) return null;
        return dt.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public boolean hasReachedVehicleLimit(User driver) {
        long count = vehicleRepository.countByDriverId(driver.getId());
        return count >= maxVehiclesPerDriver;
    }

    // ==================== 🔹 DTO CONVERSION (Null-Safe) ====================

    private VehicleResponse convertToResponse(Vehicle v, String currentUserId) {
        if (v == null) return null;

        // ✅ Null-safe Boolean checks
        boolean isVerified = Boolean.TRUE.equals(v.getIsVerified());
        boolean isActive = Boolean.TRUE.equals(v.getIsActive());  // ✅ Use getIsActive() for Boolean wrapper
        boolean isOwner = v.getDriver() != null &&
                v.getDriver().getId() != null &&
                v.getDriver().getId().equals(currentUserId);

        // ✅ Compute status
        String status = !isActive ? "inactive" : isVerified ? "verified" : "pending";

        return VehicleResponse.builder()
                .id(v.getId())
                .vehicleNumber(v.getVehicleNumber())
                .type(v.getType() != null ? v.getType().name() : null)
                .totalSeats(v.getTotalSeats())
                .imageUrl(getFullImageUrl(v.getImageUrl()))
                .isVerified(isVerified)
                .isActive(isActive)
                .driverId(v.getDriver() != null ? v.getDriver().getId() : null)  // ✅ Critical for frontend!
                .driverName(v.getDriver() != null && v.getDriver().getName() != null ?
                        maskName(v.getDriver().getName()) : null)
                .createdAt(formatTimestamp(v.getCreatedAt()))
                .updatedAt(formatTimestamp(v.getUpdatedAt()))
                .displayLabel(v.getDisplayLabel())
                .status(status)
                // ✅ Permission flags
                .canEdit(isOwner && isActive && isVerified)
                .canDelete(isOwner)
                .isAvailableForBooking(isActive && isVerified)
                .build();
    }

    private String maskName(String fullName) {
        if (!StringUtils.hasText(fullName)) return null;
        String trimmed = fullName.trim();
        if (!trimmed.contains(" ")) return trimmed;

        String[] parts = trimmed.split("\\s+", 2);
        return parts[1].isEmpty() ? parts[0] : parts[0] + " " + parts[1].charAt(0) + ".";
    }


    @Transactional(readOnly = true)
    public List<VehicleResponse> getPendingVehicles() {
        log.debug("📋 Fetching pending vehicles for admin approval");

        // ✅ Direct DB query - no in-memory filtering
        List<Vehicle> pending = vehicleRepository.findPendingVehicles();

        return pending.stream()
                .map(v -> convertToResponse(v, null))  // null = admin view, no ownership flags
                .collect(Collectors.toList());
    }

    // 📁 src/main/java/com/safar_zone_backend/services/VehicleService.java

    /**
     * ✅ NEW: Get simple list of ACTIVE + VERIFIED vehicles for package creation
     * No pagination, no complex filters - just what frontend needs for dropdown
     */
    @Transactional(readOnly = true)
    public List<VehicleResponse> getAvailableVehiclesForPackages(String driverId) {
        log.debug("📦 Fetching available vehicles for package creation - driver: {}", driverId);

        // ✅ Query: Active + Verified vehicles only, sorted by recent
        List<Vehicle> vehicles = vehicleRepository.findByDriverIdAndIsActiveAndIsVerifiedOrderByCreatedAtDesc(
                driverId, true, true);

        return vehicles.stream()
                .map(v -> convertToResponse(v, driverId))
                .collect(Collectors.toList());
    }


    // SUPER ADMIN FUCTIONS

    @Transactional(readOnly = true)
    public List<VehicleResponse> getAllVehicles() {

        return vehicleRepository.findAll()
                .stream()
                .map(v -> convertToResponse(v, null))
                .toList();
    }


    @Transactional
    public VehicleResponse rejectVehicle(String vehicleId) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        vehicle.setIsActive(false);

        Vehicle saved = vehicleRepository.save(vehicle);

        return convertToResponse(saved, null);
    }
}