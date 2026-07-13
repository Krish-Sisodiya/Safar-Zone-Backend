package com.safar_zone_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ✅ UserResponse DTO - Safe user profile for API responses
 *
 * 🔹 Design Principles:
 * - Never expose sensitive data (password, internal IDs)
 * - Always use null-safe types (Boolean, not boolean)
 * - Format dates as ISO 8601 strings for frontend compatibility
 * - Exclude null fields from JSON for cleaner responses
 *
 * 🔹 Usage:
 * - Returned by: /api/auth/me, /api/auth/login, /api/auth/register
 * - Consumed by: React Native frontend for profile display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // ✅ Exclude null fields from JSON
public class UserResponse {

    /**
     * ✅ User's unique identifier (UUID format)
     * Example: "550e8400-e29b-41d4-a716-446655440000"
     */
    private String id;

    /**
     * ✅ User's full name (as provided during registration)
     * Example: "Rahul Sharma"
     */
    private String name;

    /**
     * ✅ User's email address (normalized: lowercase + trimmed)
     * Example: "rahul@example.com"
     */
    private String email;

    /**
     * ✅ User's phone number (international format recommended)
     * Example: "+919876543210"
     */
    private String phone;

    /**
     * ✅ User's role in the system
     * Allowed values: "DRIVER", "TRAVELER", "ADMIN", "MODERATOR"
     * Frontend uses this for conditional UI rendering
     */
    private String role;

    /**
     * ✅ Is the user's account verified?
     * - true: Account verified (via OTP/email)
     * - false: Pending verification
     * - null: Unknown/unset (legacy data)
     *
     * 🔹 Use wrapper Boolean for null-safety
     */
    @Builder.Default
    private Boolean isVerified = false;

    private String driverVerificationStatus;

    private String licenseNumber;
    /**
     * ✅ Account creation timestamp in ISO 8601 format (UTC)
     * Format: "2024-01-15T10:30:00"
     * Frontend: new Date(user.createdAt) works directly
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private String createdAt;

    /**
     * ✅ Last update timestamp in ISO 8601 format (UTC)
     * Useful for: cache invalidation, "last seen" display, sync logic
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private String updatedAt;

    // ==================== 🔹 HELPER METHODS (Optional but Useful) ====================

    /**
     * ✅ Get display name for UI: name → email → "User"
     * Fallback chain ensures something always shows
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        if (email != null && !email.trim().isEmpty()) {
            // Show only part before @ for privacy
            return email.split("@")[0];
        }
        return "User";
    }

    /**
     * ✅ Check if user has DRIVER role (for conditional UI)
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean isDriver() {
        return "DRIVER".equalsIgnoreCase(role);
    }

    /**
     * ✅ Check if user has ADMIN role (for admin UI features)
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    /**
     * ✅ Check if account is fully verified AND active
     * Useful for: enabling booking features, package creation, etc.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean isAccountActive() {
        return Boolean.TRUE.equals(isVerified);
    }

    // ==================== 🔹 STATIC FACTORY METHODS (Clean Construction) ====================

    /**
     * ✅ Create minimal user response (for list views)
     */
    public static UserResponse minimal(String id, String name, String role) {
        return UserResponse.builder()
                .id(id)
                .name(name)
                .role(role)
                .build();
    }

    /**
     * ✅ Create full user response (for profile/details)
     */
    public static UserResponse full(String id, String name, String email, String phone,
                                    String role, Boolean isVerified, String createdAt, String updatedAt) {
        return UserResponse.builder()
                .id(id)
                .name(name)
                .email(email)
                .phone(phone)
                .role(role)
                .isVerified(isVerified)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}