package com.safar_zone_backend.controller;

import com.safar_zone_backend.dto.*;
import com.safar_zone_backend.entity.Role;
import com.safar_zone_backend.entity.User;
import com.safar_zone_backend.repository.UserRepository;
import com.safar_zone_backend.services.AuthService;
import com.safar_zone_backend.util.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:8081,exp://*,https://safarzone.com}")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    // ==================== 🔹 OTP FLOW ====================

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody OtpRequest request) {
        String email = normalizeEmail(request.getEmail());
        log.info("📤 POST /send-otp - Email: {}", email);

        try {
            authService.sendOtp(email, request.getRole());
            log.info("✅ OTP sent to {}", email);
            return ResponseEntity.ok(ApiResponse.success(null, "OTP sent to your email"));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid OTP request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("❌ Failed to send OTP: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to send OTP. Please try again.", 500));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String email = normalizeEmail(request.getEmail());
        log.info("🔐 POST /verify-otp - Email: {}", email);

        try {
            AuthResponse result = authService.verifyOtp(email, request.getOtp());
            log.info("✅ OTP verified: {} → exists={}", email, result.getExists());

            // ✅ Add role-based redirect hint for frontend convenience
            String redirectHint = determineRedirectHint(result);

            return ResponseEntity.ok(ApiResponse.success(
                    result,
                    result.getExists() ? "Login successful" : "Proceed to registration",
                    redirectHint  // ✅ Optional: frontend can use this for auto-redirect
            ));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid OTP: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid or expired OTP. Please request a new one.", 400));
        } catch (Exception e) {
            log.error("❌ OTP verification failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Verification failed. Please try again.", 500));
        }
    }

    // ==================== 🔹 LOGIN & REGISTER ====================

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        log.info("🔐 POST /login - Email: {}", email);

        try {
            AuthResponse result = authService.login(request);

            // ✅ Null-safe logging
            String role = result.getUser() != null ? result.getUser().getRole() : "UNKNOWN";
            log.info("✅ Login successful: {} (role: {})", email, role);

            return ResponseEntity.ok(ApiResponse.success(result, "Login successful"));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Login failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid email or password", 400));
        } catch (Exception e) {
            log.error("❌ Login failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Login failed. Please try again.", 500));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        log.info("📝 POST /register - Email: {}", email);

        try {
            AuthResponse result = authService.register(request);

            // ✅ Null-safe logging
            String userId = result.getUser() != null ? result.getUser().getId() : "unknown";
            log.info("✅ Registration successful: {} (id: {})", email, userId);

            return ResponseEntity.ok(ApiResponse.success(result, "Registration successful"));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("❌ Registration failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Registration failed. Please try again.", 500));
        }
    }

    // ==================== 🔹 USER PROFILE ====================

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || userDetails.getUserId() == null) {
            log.warn("⚠️ Unauthorized access to /me endpoint");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required", 401));
        }

        log.debug("👤 GET /me - User: {} (email: {})", userDetails.getUserId(), userDetails.getEmail());

        try {
            // ✅ Fetch latest user data from DB (ensures up-to-date info)
            User user = userRepository.findById(userDetails.getUserId())
                    .orElseThrow(() -> {
                        log.warn("⚠️ User not found: {}", userDetails.getUserId());
                        return new RuntimeException("User not found");
                    });

            UserResponse response =
                    UserResponse.builder()
                            .id(user.getId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .phone(user.getPhone())
                            .licenseNumber(
                                    user.getLicenseNumber()
                            )
                            .driverVerificationStatus(
                                    user.getDriverVerificationStatus()
                                            .name()
                            )
                            .role(
                                    user.getRole().name()
                            )
                            .isVerified(
                                    user.getIsVerified()
                            )
                            .createdAt(
                                    user.getCreatedAt().toString()
                            )
                            .updatedAt(
                                    user.getUpdatedAt().toString()
                            )
                            .build();

            log.debug("✅ Profile fetched for user: {}", userDetails.getUserId());
            return ResponseEntity.ok(ApiResponse.success(response, "User profile fetched"));

        } catch (RuntimeException e) {
            log.warn("⚠️ Profile fetch failed: {}", e.getMessage());
            return ResponseEntity.status(404).body(ApiResponse.error("User not found", 404));
        } catch (Exception e) {
            log.error("❌ Profile fetch failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch profile. Please try again.", 500));
        }
    }

    // ==================== 🔹 UTILITY ENDPOINTS ====================

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Void>> health() {
        log.debug("🏥 Health check requested");
        return ResponseEntity.ok(ApiResponse.success(null, "Auth service is running"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails != null ? userDetails.getUserId() : "anonymous";
        log.info("🚪 POST /logout - User: {}", userId);

        // ✅ JWT is stateless - client deletes token from storage
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<EmailCheckResponse>> checkEmail(
            @RequestParam String email) {

        String normalizedEmail = normalizeEmail(email);
        log.debug("🔍 Checking email registration: {}", normalizedEmail);

        try {
            boolean isRegistered = authService.isEmailRegistered(normalizedEmail);
            return ResponseEntity.ok(ApiResponse.success(
                    new EmailCheckResponse(isRegistered),
                    isRegistered ? "Email is registered" : "Email is available"
            ));
        } catch (Exception e) {
            log.error("❌ Email check failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to check email. Please try again.", 500));
        }
    }

    // ==================== 🔹 HELPER METHODS ====================

    /**
     * ✅ Normalize email: lowercase + trim (consistent across all endpoints)
     */
    private String normalizeEmail(String email) {
        if (email == null) return null;
        return email.toLowerCase().trim();
    }

    /**
     * ✅ Determine frontend redirect hint based on auth response
     * Helps frontend auto-redirect without extra logic
     */
    private String determineRedirectHint(AuthResponse result) {
        if (result == null || !result.getExists()) {
            return null;  // New user → frontend handles registration redirect
        }

        UserResponse user = result.getUser();
        if (user == null || user.getRole() == null) {
            return "/(traveler)";  // Default fallback
        }

        return switch (user.getRole().toUpperCase()) {
            case "DRIVER" -> "/(driver)";
            case "ADMIN" -> "/(admin)";
            default -> "/(traveler)";
        };
    }

}