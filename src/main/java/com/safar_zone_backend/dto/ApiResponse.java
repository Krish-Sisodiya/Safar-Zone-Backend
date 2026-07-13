package com.safar_zone_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * ✅ Generic API Response Wrapper
 *
 * 🔹 Features:
 * - Null-safe fields (Boolean wrapper)
 * - Generic type support (T data)
 * - Optional redirectHint for auth flow convenience
 * - JSON null fields excluded for cleaner responses
 *
 * 🔹 Usage:
 * - Success: ApiResponse.success(data, "message")
 * - Success with redirect: ApiResponse.success(data, "message", "/(traveler)")
 * - Error: ApiResponse.error("message", 400)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // ✅ Exclude null fields from JSON
public class ApiResponse<T> {

    /**
     * ✅ Request success status
     */
    private Boolean success;

    /**
     * ✅ Human-readable message for frontend display
     */
    private String message;

    /**
     * ✅ Response payload (generic type)
     */
    private T data;

    /**
     * ✅ Response timestamp (ISO format via Jackson)
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * ✅ HTTP status code for programmatic handling
     */
    private Integer statusCode;

    /**
     * ✅ Optional: Frontend redirect hint for auth flow
     * Values: "/(traveler)", "/(driver)", "/(admin)", null
     */
    private String redirectHint;

    // ==================== 🔹 FACTORY METHODS ====================

    /**
     * ✅ Success response (basic)
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .statusCode(200)
                .build();
    }

    /**
     * ✅ Success response with redirect hint (for auth flow)
     */
    public static <T> ApiResponse<T> success(T data, String message, String redirectHint) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .redirectHint(redirectHint)
                .timestamp(LocalDateTime.now())
                .statusCode(200)
                .build();
    }

    /**
     * ✅ Error response (default 400)
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .statusCode(400)
                .build();
    }

    /**
     * ✅ Error response with custom status code
     */
    public static <T> ApiResponse<T> error(String message, int statusCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .statusCode(statusCode)
                .build();
    }

    // ==================== 🔹 HELPER METHODS ====================

    /**
     * ✅ Check if response indicates success
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }

    /**
     * ✅ Check if response indicates error
     */
    public boolean isError() {
        return Boolean.FALSE.equals(success);
    }
}