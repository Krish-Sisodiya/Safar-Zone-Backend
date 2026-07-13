package com.safar_zone_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.safar_zone_backend.entity.Vehicle;
import lombok.*;

/**
 * ✅ VehicleResponse DTO - PRODUCTION READY
 *
 * 🔹 Purpose: Safe API response for frontend (never expose entity directly)
 * 🔹 Features:
 *   - driverId included for frontend filtering ✅
 *   - Computed 'status' field for easy UI badges
 *   - Permission flags (canEdit/canDelete) for conditional rendering
 *   - Null-safe getters for Boolean fields
 *   - Static factory method for clean entity → DTO conversion
 *   - ISO 8601 date format for consistent frontend parsing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // ✅ Don't send null fields to frontend
public class VehicleResponse {

    // ==================== 🔹 CORE IDENTIFIERS ====================

    /**
     * ✅ Vehicle UUID (primary key)
     * Example: "550e8400-e29b-41d4-a716-446655440000"
     */
    private String id;

    /**
     * ✅ Vehicle registration number (sanitized, uppercase)
     * Example: "DL01AB1234"
     */
    private String vehicleNumber;

    /**
     * ✅ Vehicle type as string (enum name)
     * Values: "CAR", "BIKE", "AUTO", "BUS", "TEMPO"
     */
    private String type;

    /**
     * ✅ Total passenger seats (1-50)
     */
    private Integer totalSeats;

    // ==================== 🔹 MEDIA & STATUS ====================

    /**
     * ✅ Full image URL (absolute) or null
     * Example: "https://api.safarzone.com/uploads/vehicles/abc.jpg"
     */
    private String imageUrl;

    /**
     * ✅ Admin verification status
     * - true: Approved for booking
     * - false: Pending admin review
     */
    @Builder.Default
    private Boolean isVerified = false;

    /**
     * ✅ Active status (soft delete flag)
     * - true: Visible in lists
     * - false: Soft-deleted/hidden
     */
    @Builder.Default
    private Boolean isActive = true;

    // ==================== 🔹 OWNERSHIP (CRITICAL FOR FRONTEND) ====================

    /**
     * ✅ 🔥 CRITICAL FIX: Driver/Owner UUID
     * Frontend uses this to filter: `v.driverId === currentUser.id`
     *
     * ⚠️ Without this field, your vehicle list will appear empty!
     */
    private String driverId;

    /**
     * ✅ Driver name (masked for privacy) or null
     * Example: "Rahul S." (not full name/email)
     */
    private String driverName;

    // ==================== 🔹 TIMESTAMPS (ISO 8601 Format) ====================

    /**
     * ✅ Creation timestamp in ISO 8601 format
     * Frontend: `new Date(vehicle.createdAt)` works directly
     * Example: "2024-01-15T10:30:00"
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private String createdAt;

    /**
     * ✅ Last update timestamp in ISO 8601 format
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private String updatedAt;

    // ==================== 🔹 UI HELPERS ====================

    /**
     * ✅ Pre-formatted display label for quick UI rendering
     * Example: "DL01AB1234 • CAR • 4 Seats"
     * Frontend can use directly without string formatting
     */
    private String displayLabel;

    /**
     * ✅ Computed status string for badge rendering
     * Values: "verified" | "pending" | "inactive"
     *
     * Frontend usage:
     * ```
     * const badgeColor = {
     *   verified: 'green',
     *   pending: 'amber',
     *   inactive: 'gray'
     * }[vehicle.status];
     * ```
     */
    private String status;  // "verified" | "pending" | "inactive"

    // ==================== 🔹 PERMISSION FLAGS (For Conditional UI) ====================

    /**
     * ✅ Can current user edit this vehicle?
     * - true: Show edit button
     * - false: Hide/disable edit
     *
     * Calculated as: isActive && isVerified && isOwner
     */
    @Builder.Default
    private Boolean canEdit = false;

    /**
     * ✅ Can current user delete this vehicle?
     * - true: Show delete button
     * - false: Hide/delete disabled
     *
     * Calculated as: isOwner (even inactive vehicles can be deleted by owner)
     */
    @Builder.Default
    private Boolean canDelete = false;

    /**
     * ✅ Can vehicle be used for package creation?
     * - true: Show in "Select Vehicle" dropdown for packages
     * - false: Hide from package flow
     *
     * Calculated as: isActive && isVerified
     */
    @Builder.Default
    private Boolean isAvailableForBooking = false;

    // ==================== 🔹 NULL-SAFE GETTERS (Prevent NPE) ====================

    public boolean isVerified() {
        return Boolean.TRUE.equals(this.isVerified);
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(this.isActive);
    }

    public boolean isCanEdit() {
        return Boolean.TRUE.equals(this.canEdit);
    }

    public boolean isCanDelete() {
        return Boolean.TRUE.equals(this.canDelete);
    }

    public boolean isAvailableForBooking() {
        return Boolean.TRUE.equals(this.isAvailableForBooking);
    }

    // ==================== 🔹 STATIC FACTORY METHOD (Clean Conversion) ====================

    /**
     * ✅ Convert Vehicle entity → VehicleResponse DTO
     *
     * 🔹 Handles: null safety, URL generation, status computation, permissions
     * 🔹 Usage: `VehicleResponse.fromEntity(vehicle, currentUserId)`
     *
     * @param entity The Vehicle entity to convert
     * @param currentUserId ID of the requesting user (for permission checks)
     * @return Fully populated VehicleResponse DTO
     */
    public static VehicleResponse fromEntity(Vehicle entity, String currentUserId) {
        if (entity == null) {
            return null;
        }

        // ✅ Compute status string
        String status = computeStatus(entity);

        // ✅ Check ownership for permissions
        boolean isOwner = entity.getDriver() != null &&
                entity.getDriver().getId() != null &&
                entity.getDriver().getId().equals(currentUserId);

        // ✅ Build response with all fields
        return VehicleResponse.builder()
                // Core fields
                .id(entity.getId())
                .vehicleNumber(entity.getVehicleNumber())
                .type(entity.getType() != null ? entity.getType().name() : null)
                .totalSeats(entity.getTotalSeats())

                // Media & status
                .imageUrl(entity.getImageUrl())  // Should already be full URL from service
                .isVerified(entity.isVerified())  // ✅ Null-safe getter
                .isActive(entity.isActive())      // ✅ Null-safe getter

                // Ownership (CRITICAL!)
                .driverId(entity.getDriver() != null ? entity.getDriver().getId() : null)
                .driverName(maskDriverName(entity.getDriver()))

                // Timestamps (formatted)
                .createdAt(formatTimestamp(entity.getCreatedAt()))
                .updatedAt(formatTimestamp(entity.getUpdatedAt()))

                // UI helpers
                .displayLabel(entity.getDisplayLabel())
                .status(status)

                // Permission flags
                .canEdit(isOwner && entity.isActive() && entity.isVerified())
                .canDelete(isOwner)  // Owner can delete even inactive vehicles
                .isAvailableForBooking(entity.isActive() && entity.isVerified())

                .build();
    }

    /**
     * ✅ Simplified version when permissions aren't needed (e.g., admin views)
     */
    public static VehicleResponse fromEntity(Vehicle entity) {
        return fromEntity(entity, null);  // No permission checks
    }

    // ==================== 🔹 PRIVATE HELPERS ====================

    /**
     * ✅ Compute human-readable status string
     */
    private static String computeStatus(Vehicle v) {
        if (v == null) return "unknown";
        if (!v.isActive()) return "inactive";
        if (v.isVerified()) return "verified";
        return "pending";
    }

    /**
     * ✅ Mask driver name for privacy (show only first name + initial)
     * Example: "Rahul Sharma" → "Rahul S."
     */
    private static String maskDriverName(com.safar_zone_backend.entity.User driver) {
        if (driver == null || driver.getName() == null) {
            return null;
        }
        String name = driver.getName().trim();
        if (!name.contains(" ")) {
            return name;  // Single name, return as-is
        }
        String[] parts = name.split("\\s+", 2);
        return parts[0] + " " + parts[1].charAt(0) + ".";
    }

    /**
     * ✅ Format LocalDateTime to ISO 8601 string (UTC timezone)
     */
    private static String formatTimestamp(java.time.LocalDateTime dt) {
        if (dt == null) {
            return null;
        }
        return dt.atZone(java.time.ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    // ==================== 🔹 UTILITY: Batch Conversion ====================

    /**
     * ✅ Convert list of entities → list of DTOs
     * Usage: `VehicleResponse.fromEntities(vehicleList, userId)`
     */
    public static java.util.List<VehicleResponse> fromEntities(
            java.util.List<Vehicle> entities,
            String currentUserId) {

        if (entities == null) {
            return java.util.Collections.emptyList();
        }

        return entities.stream()
                .map(e -> fromEntity(e, currentUserId))
                .filter(java.util.Objects::nonNull)
                .toList();  // Java 16+ immutable list
    }
}