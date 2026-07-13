package com.safar_zone_backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ✅ Vehicle Entity - PRODUCTION READY & OPTIMIZED
 *
 * 🔹 Key Features:
 * - JSON serialization safe (@JsonIgnore, @JsonBackReference)
 * - Frontend-friendly date formatting (@JsonFormat)
 * - Clear @SQLRestriction documentation + admin workaround
 * - Comprehensive helper methods for business logic
 * - Null-safe getters and builders
 * - Optimized indexes for common queries
 *
 * ⚠️ IMPORTANT: @SQLRestriction("is_active = true") means:
 * - All JPQL queries automatically filter out inactive vehicles
 * - For admin queries needing ALL vehicles, use:
 *   1. Native queries (@Query(nativeQuery = true))
 *   2. Or create separate entity without @SQLRestriction
 *   3. Or use @Where clause with conditional logic
 */
@Entity
@Table(
        name = "vehicles",
        indexes = {
                @Index(name = "idx_driver_vehicle_unique", columnList = "driver_id, vehicle_number", unique = true),
                @Index(name = "idx_driver_id", columnList = "driver_id"),
                @Index(name = "idx_vehicle_number", columnList = "vehicle_number"),
                @Index(name = "idx_verified_status", columnList = "is_verified, is_active"),
                @Index(name = "idx_created_at", columnList = "created_at DESC"),
                @Index(name = "idx_type", columnList = "type"),
                @Index(name = "idx_active_verified", columnList = "is_active, is_verified") // ✅ Added for common filter combo
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_driver_vehicle_number", columnNames = {"driver_id", "vehicle_number"})
        }
)
@Data
@Builder(toBuilder = true)  // ✅ Enable toBuilder for immutable updates
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)  // ✅ Safe deserialization
@SQLRestriction("is_active = true")  // ⚠️ Auto-filters inactive vehicles in JPQL queries
public class Vehicle {

    // ==================== PRIMARY KEY ====================
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)", updatable = false)
    private String id;

    // ==================== RELATIONSHIPS ====================
    /**
     * ✅ @JsonBackReference: Prevents infinite recursion in JSON serialization
     * ✅ FetchType.LAZY: Don't load driver unless explicitly fetched with @EntityGraph
     *
     * 🔹 Frontend will receive: { "driverId": "uuid", "driverName": "John" } via DTO
     * 🔹 Never expose full User object in API responses
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "driver_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_vehicle_driver")
    )
    @JsonBackReference  // ✅ Critical: Prevents circular JSON serialization
    private User driver;

    // ==================== CORE VEHICLE FIELDS ====================
    @Column(name = "vehicle_number", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private String vehicleNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, columnDefinition = "VARCHAR(10)")
    private VehicleType type;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    // ==================== MEDIA & VERIFICATION ====================
    @Column(name = "image_url", length = 500, columnDefinition = "VARCHAR(500)")
    private String imageUrl;

    @Column(name = "is_verified", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    @Builder.Default
    private Boolean isVerified = false;

    /**
     * ✅ IMPORTANT: Java field = 'active', DB column = 'is_active'
     * This mismatch is intentional for cleaner Java code.
     *
     * 🔹 In JPQL queries: use 'v.active' (Java field name)
     * 🔹 In Native SQL: use 'is_active' (DB column name)
     * 🔹 @SQLRestriction uses DB column name: "is_active = true"
     */
    @Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    @Builder.Default
    private Boolean isActive = true;

    // ==================== AUDIT TIMESTAMPS ====================
    /**
     * ✅ @JsonFormat: Frontend-friendly ISO 8601 with timezone
     * Example output: "2024-01-15T10:30:00+05:30"
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP")
    // Entity - remove timezone from @JsonFormat
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")  // ✅ No fake offset
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Kolkata")
    private LocalDateTime updatedAt;

    // ==================== OPTIONAL AUDIT FIELDS ====================
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "admin_notes", length = 500, columnDefinition = "TEXT")
    private String adminNotes;

    // ==================== LIFECYCLE CALLBACKS ====================
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isActive  == null) {
            this.isActive  = true;
        }
        if (this.isVerified == null) {
            this.isVerified = false;
        }
        if (this.vehicleNumber != null) {
            // ✅ Sanitize: uppercase, remove spaces, trim
            this.vehicleNumber = this.vehicleNumber.toUpperCase().replaceAll("\\s+", "");
        }
        log.debug("🚗 Vehicle entity created: {} (driver: {})",
                this.vehicleNumber,
                this.driver != null ? this.driver.getId() : "unknown");
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        log.debug("🔄 Vehicle entity updated: {} (id: {})",
                this.vehicleNumber,
                this.id);
    }

    // ==================== ✅ NULL-SAFE GETTERS ====================

    public String getVehicleNumber() {
        return vehicleNumber != null ? vehicleNumber : "";
    }

    public VehicleType getType() {
        return type;  // Enum can't be null if column is nullable=false
    }

    public Integer getTotalSeats() {
        return totalSeats != null ? totalSeats : 0;
    }

    public String getImageUrl() {
        return imageUrl;  // Can be null (optional field)
    }

    public boolean isVerified() {
        return Boolean.TRUE.equals(this.isVerified);  // ✅ Null-safe
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(this.isActive );  // ✅ Null-safe
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ==================== ✅ BUSINESS LOGIC HELPERS ====================

    /**
     * ✅ Check if vehicle is available for booking/creating packages
     */
    @Transient
    @JsonIgnore  // ✅ Don't expose internal logic in JSON
    public boolean isAvailableForBooking() {
        return isVerified() && isActive();
    }

    /**
     * ✅ Check if vehicle is pending admin verification
     */
    @Transient
    @JsonIgnore
    public boolean isPendingVerification() {
        return !isVerified() && isActive();
    }

    /**
     * ✅ Check if vehicle can be edited by driver
     * (Only pending vehicles can be edited; verified vehicles need admin approval for changes)
     */
    @Transient
    @JsonIgnore
    public boolean canBeEditedByDriver() {
        return isPendingVerification();
    }

    /**
     * ✅ Get vehicle type in lowercase for frontend filtering
     */
    @Transient
    public String getTypeLowerCase() {
        return this.type != null ? this.type.name().toLowerCase() : "unknown";
    }

    /**
     * ✅ Get formatted display label for UI
     * Example: "DL01AB1234 • CAR • 4 Seats"
     */
    @Transient
    public String getDisplayLabel() {
        return String.format("%s • %s • %d Seats",
                getVehicleNumber(),
                getType() != null ? getType().name() : "Unknown",
                getTotalSeats());
    }

    /**
     * ✅ Get driver ID safely (for DTO conversion)
     */
    @Transient
    public String getDriverId() {
        return this.driver != null ? this.driver.getId() : null;
    }

    /**
     * ✅ Get driver name safely (for DTO conversion)
     */
    @Transient
    public String getDriverName() {
        return this.driver != null ? this.driver.getName() : "Unknown Driver";
    }

    /**
     * ✅ Get formatted creation date for frontend
     * Example: "15 Jan 2024"
     */
    // 🔹 Class-level static constant (thread-safe)
    private static final DateTimeFormatter FORMATTED_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH);

    // 🔹 Updated method
    public String getFormattedCreatedAt() {
        return createdAt != null ? createdAt.format(FORMATTED_DATE_FORMATTER) : "N/A";
    }

    // ==================== ✅ STATE MUTATION METHODS ====================

    /**
     * ✅ Soft delete: Mark vehicle as inactive
     * 🔹 Data preserved for audit, hidden from normal queries due to @SQLRestriction
     */
    public void deactivate() {
        if (Boolean.TRUE.equals(this.isActive )) {
            this.isActive  = false;
            this.updatedAt = LocalDateTime.now();
            log.info("🗑️ Vehicle deactivated: {} (id: {})", this.vehicleNumber, this.id);
        }
    }

    /**
     * ✅ Reactivate a soft-deleted vehicle
     */
    public void activate() {
        if (!Boolean.TRUE.equals(this.isActive )) {
            this.isActive  = true;
            this.updatedAt = LocalDateTime.now();
            log.info("✅ Vehicle reactivated: {} (id: {})", this.vehicleNumber, this.id);
        }
    }

    /**
     * ✅ Mark vehicle as verified by admin
     */
    public void verify() {
        if (!Boolean.TRUE.equals(this.isVerified)) {
            this.isVerified = true;
            this.updatedAt = LocalDateTime.now();
            log.info("✓ Vehicle verified: {} (id: {})", this.vehicleNumber, this.id);
        }
    }

    /**
     * ✅ Update image URL with validation
     */
    public void updateImageUrl(String newImageUrl) {
        if (newImageUrl != null && !newImageUrl.trim().isEmpty()) {
            this.imageUrl = newImageUrl.trim();
            this.updatedAt = LocalDateTime.now();
            log.debug("🖼️ Image updated for vehicle: {}", this.vehicleNumber);
        }
    }

    /**
     * ✅ Update vehicle details (partial update support)
     * 🔹 Use builder pattern for immutable-style updates
     */
    public void updateDetails(String vehicleNumber, VehicleType type, Integer totalSeats) {
        if (vehicleNumber != null && !vehicleNumber.isBlank()) {
            this.vehicleNumber = vehicleNumber.toUpperCase().replaceAll("\\s+", "");
        }
        if (type != null) {
            this.type = type;
        }
        if (totalSeats != null && totalSeats > 0) {
            this.totalSeats = totalSeats;
        }
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== ✅ EQUALS & HASHCODE (JPA Best Practice) ====================

    /**
     * ✅ Only compare by ID for JPA entities
     * 🔹 Prevents issues with lazy-loaded fields
     * 🔹 Safe for use in collections (Set, Map keys)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehicle vehicle)) return false;
        // ✅ Only compare if both IDs are non-null
        return id != null && id.equals(vehicle.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(id);  // ✅ ID-based hash
    }

    // ==================== ✅ SAFE toString() FOR LOGGING ====================

    /**
     * ✅ Never log sensitive data (driver details, etc.)
     * 🔹 Safe for debug logs, monitoring, error tracking
     */
    @Override
    public String toString() {
        return String.format("Vehicle{id='%s', number='%s', type=%s, seats=%d, verified=%s, active=%s}",
                id,
                vehicleNumber,
                type,
                totalSeats,
                isVerified,
                isActive
                // ❌ Don't include: driver, imageUrl, adminNotes (sensitive/large)
        );
    }

    // ==================== ✅ DTO CONVERSION HELPERS ====================

    /**
     * ✅ Convert to minimal DTO for list views (performance optimized)
     * 🔹 Only includes fields needed for vehicle list display
     */
    @Transient
    public VehicleListItem toListItem() {
        return new VehicleListItem(
                this.id,
                this.getVehicleNumber(),
                this.getTypeLowerCase(),
                this.getTotalSeats(),
                this.getImageUrl(),
                this.isVerified(),
                this.getFormattedCreatedAt()
        );
    }

    /**
     * ✅ Convert to full DTO for detail views
     * 🔹 Includes all non-sensitive fields
     */
    @Transient
    public VehicleDetail toDetail() {
        return new VehicleDetail(
                this.id,
                this.getVehicleNumber(),
                this.getType(),
                this.getTotalSeats(),
                this.getImageUrl(),
                this.isVerified(),
                this.isActive(),
                this.getDriverId(),
                this.getDriverName(),
                this.getCreatedAt(),
                this.getUpdatedAt(),
                this.getDisplayLabel()
        );
    }

    // ==================== ✅ INNER RECORDS FOR DTOs (Optional) ====================
    // 🔹 Alternative: Create separate DTO classes in com.safar_zone_backend.dto package

    public record VehicleListItem(
            String id,
            String vehicleNumber,
            String type,
            Integer totalSeats,
            String imageUrl,
            Boolean isVerified,
            String createdAt
    ) {}

    public record VehicleDetail(
            String id,
            String vehicleNumber,
            VehicleType type,
            Integer totalSeats,
            String imageUrl,
            Boolean isVerified,
            Boolean isActive,
            String driverId,
            String driverName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String displayLabel
    ) {}
}