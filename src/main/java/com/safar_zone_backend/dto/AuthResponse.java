package com.safar_zone_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ✅ AuthResponse DTO - Response for authentication flows
 *
 * 🔹 Usage Scenarios:
 * - OTP Verify (existing user): { exists: true, token: "...", user: {...} }
 * - OTP Verify (new user): { exists: false, email: "user@example.com" }
 * - Login/Register: { exists: true, token: "...", user: {...} }
 *
 * 🔹 JSON Behavior:
 * - Null fields are excluded from response (NON_NULL)
 * - Field names in JSON match Java names (no @JsonProperty needed)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // ✅ Exclude null fields from JSON
public class AuthResponse {

    /**
     * ✅ Does the user account exist?
     * - true: User found → token + user details included
     * - false: New user → email included for registration flow
     */
    @Builder.Default
    private Boolean exists = false;  // ✅ Use wrapper Boolean for null-safety

    /**
     * ✅ JWT token for authenticated requests
     * - Present only when exists=true and auth successful
     * - Format: "Bearer eyJhbGciOiJIUzI1NiJ9..."
     */
    private String token;

    /**
     * ✅ User profile details
     * - Present only when exists=true
     * - Contains: id, name, email, role, etc.
     */
    private UserResponse user;

    /**
     * ✅ Email for new user registration flow
     * - Present only when exists=false (OTP verified, but account doesn't exist yet)
     * - Frontend uses this to pre-fill registration form
     */
    private String email;

    // ==================== 🔹 HELPER METHODS (Optional but Useful) ====================

    /**
     * ✅ Check if this is a successful authentication response
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean isAuthenticated() {
        return Boolean.TRUE.equals(exists) && token != null && user != null;
    }

    /**
     * ✅ Check if this is a new user (needs registration)
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean isNewUser() {
        return Boolean.FALSE.equals(exists) && email != null;
    }

    /**
     * ✅ Static factory method for existing user login
     */
    public static AuthResponse forExistingUser(String token, UserResponse user) {
        return AuthResponse.builder()
                .exists(true)
                .token(token)
                .user(user)
                .build();
    }

    /**
     * ✅ Static factory method for new user (post-OTP verification)
     */
    public static AuthResponse forNewUser(String email) {
        return AuthResponse.builder()
                .exists(false)
                .email(email)
                .build();
    }
}