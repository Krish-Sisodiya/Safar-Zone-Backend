package com.safar_zone_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ✅ Email Check Response DTO
 *
 * 🔹 Purpose:
 * - Response for GET /api/auth/check-email endpoint
 * - Frontend uses this to show "Email available" or "Email registered"
 *
 * 🔹 JSON Example:
 * {
 *   "registered": true,
 *   "message": "Email is already registered"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailCheckResponse {

    /**
     * ✅ Is the email already registered in the system?
     * - true: Email exists → show "Login instead"
     * - false: Email available → allow registration
     */
    private boolean registered;

    /**
     * ✅ Optional message for frontend display
     */
    private String message;

    // ==================== 🔹 FACTORY METHODS ====================

    /**
     * ✅ Create response for registered email
     */
    public static EmailCheckResponse registered(String email) {
        return EmailCheckResponse.builder()
                .registered(true)
                .message("Email is already registered. Please login instead.")
                .build();
    }

    /**
     * ✅ Create response for available email
     */
    public static EmailCheckResponse available(String email) {
        return EmailCheckResponse.builder()
                .registered(false)
                .message("Email is available. You can proceed with registration.")
                .build();
    }

    /**
     * ✅ Simple constructor (if you don't need message)
     */
    public EmailCheckResponse(boolean registered) {
        this.registered = registered;
        this.message = registered
                ? "Email is already registered"
                : "Email is available";
    }
}