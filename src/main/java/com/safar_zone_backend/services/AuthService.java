package com.safar_zone_backend.services;

import com.safar_zone_backend.dto.*;
import com.safar_zone_backend.entity.DriverVerificationStatus;
import com.safar_zone_backend.entity.Role;
import com.safar_zone_backend.entity.User;
import com.safar_zone_backend.repository.UserRepository;
import com.safar_zone_backend.util.JwtUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final OtpService otpService;  // ✅ Delegate OTP logic
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9]{10,15}$");
    private static final int MIN_PASSWORD_LENGTH = 8;

    // ==================== 🔹 OTP FLOW (Delegated to OtpService) ====================

    public void sendOtp(String email, String roleStr) {
        log.info("📤 AuthService: Sending OTP to {}", email);
        otpService.sendOtp(email, roleStr);  // ✅ Delegate
    }

    @Transactional(readOnly = true)
    public AuthResponse verifyOtp(String email, String otpCode) {
        log.info("🔐 AuthService: Verifying OTP for {}", email);

        // ✅ Use OtpService for verification
        OtpService.OtpVerificationResult result = otpService.verifyOtp(email, otpCode);

        if (!result.success()) {
            throw new IllegalArgumentException(getOtpErrorMessage(result.reason()));
        }

        String normalizedEmail = result.email();

        // ✅ Use findByEmailIgnoreCase for consistency
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);

        if (userOpt.isPresent()) {
            // ✅ Existing user → generate JWT with ALL claims
            User user = userOpt.get();
            log.info("✅ Existing user: {} (role: {})", normalizedEmail, user.getRole());

            String token = generateTokenForUser(user);  // ✅ Includes phone claim
            return AuthResponse.forExistingUser(token, toUserResponse(user));
        } else {
            // ✅ New user → proceed to registration
            log.info("🆕 New user: {}", normalizedEmail);
            return AuthResponse.forNewUser(normalizedEmail);
        }
    }

    private String getOtpErrorMessage(String reason) {
        return switch (reason) {
            case "INVALID" -> "Invalid OTP format. Enter 6 digits.";
            case "EXPIRED" -> "OTP expired. Request a new one.";
            case "ALREADY_USED" -> "OTP already used. Request a new one.";
            case "NOT_FOUND" -> "Invalid OTP. Please try again.";
            default -> "Verification failed. Try again.";
        };
    }

    // ==================== 🔹 REGISTRATION ====================

    public AuthResponse register(RegisterRequest req) {
        log.info("📝 Registering: {}", req.getEmail());
        validateRegisterRequest(req);

        String normalizedEmail = normalizeEmail(req.getEmail());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Email already registered. Please login.");
        }

        User user = User.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(req.getPassword()))
                .name(StringUtils.trimWhitespace(req.getName()))
                .phone(StringUtils.trimWhitespace(req.getPhone()))
                .role(parseRole(req.getRole()))
                .isVerified(true)  // ✅ Auto-verify after OTP
                .driverVerificationStatus(
                        DriverVerificationStatus.NOT_SUBMITTED
                )
                .licenseNumber(req.getLicenseNumber())
                .build();

        User savedUser = userRepository.save(user);
        log.info("✅ Registered: {} (id: {})", normalizedEmail, savedUser.getId());

        String token = generateTokenForUser(savedUser);  // ✅ Includes phone claim
        return AuthResponse.forExistingUser(token, toUserResponse(savedUser));
    }

    // ==================== 🔹 PASSWORD LOGIN ====================

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        log.info("🔐 Login: {}", req.getEmail());

        if (!StringUtils.hasText(req.getEmail()) || !StringUtils.hasText(req.getPassword())) {
            throw new IllegalArgumentException("Email and password required");
        }

        String normalizedEmail = normalizeEmail(req.getEmail());

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new EntityNotFoundException("Invalid email or password"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (Boolean.FALSE.equals(user.getIsVerified())) {
            throw new IllegalArgumentException("Please verify your account first");
        }

        log.info("✅ Login: {} (role: {})", normalizedEmail, user.getRole());
        String token = generateTokenForUser(user);  // ✅ Includes phone claim
        return AuthResponse.forExistingUser(token, toUserResponse(user));
    }

    // ==================== 🔹 TOKEN GENERATION (✅ KEY UPDATE) ====================

    /**
     * ✅ Generate JWT with ALL claims: userId, email, phone, role
     * Frontend uses these for data filtering
     */
    // 📁 src/main/java/com/safar_zone_backend/services/AuthService.java

    private String generateTokenForUser(User user) {
        if (user == null || user.getId() == null || user.getEmail() == null) {
            throw new IllegalStateException("Cannot generate token: invalid user");
        }

        // ✅ DEBUG: Log role before generating token
        String role = user.getRole() != null ? user.getRole().name() : Role.TRAVELER.name();
        String phone = user.getPhone() != null ? user.getPhone() : "";

        log.info("🔑 Generating token for user: {} | role: '{}' | phone: '{}'",
                user.getId(), role, phone);

        return jwtUtil.generateToken(user.getId(), user.getEmail(), phone, role);
    }

    // ==================== 🔹 DTO CONVERSION ====================

    private UserResponse toUserResponse(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .licenseNumber(
                        user.getLicenseNumber()
                )
                .role(user.getRole() != null ? user.getRole().name() : null)
                .isVerified(Boolean.TRUE.equals(user.getIsVerified()))
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .driverVerificationStatus(

                        user.getDriverVerificationStatus() != null

                                ? user.getDriverVerificationStatus().name()

                                : DriverVerificationStatus.NOT_SUBMITTED.name()

                )
                .build();
    }

    // ==================== 🔹 HELPERS ====================

    private Role parseRole(String roleStr) {
        if (!StringUtils.hasText(roleStr)) return Role.TRAVELER;
        try {
            return Role.valueOf(roleStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid role: {}", roleStr);
            return Role.TRAVELER;
        }
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase().trim();
    }

    private boolean isValidEmail(String email) {
        return StringUtils.hasText(email) && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private boolean isValidPhone(String phone) {
        return !StringUtils.hasText(phone) || PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    private void validateRegisterRequest(RegisterRequest req) {
        if (req == null) throw new IllegalArgumentException("Request cannot be empty");
        if (!isValidEmail(req.getEmail())) throw new IllegalArgumentException("Invalid email");
        validatePassword(req.getPassword());
        if (!StringUtils.hasText(req.getName()) || req.getName().trim().length() < 2) {
            throw new IllegalArgumentException("Name must be 2+ characters");
        }
        if (StringUtils.hasText(req.getPhone()) && !isValidPhone(req.getPhone())) {
            throw new IllegalArgumentException("Invalid phone number");
        }
        parseRole(req.getRole());
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password)) throw new IllegalArgumentException("Password required");
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be 8+ characters");
        }
    }

    // ==================== 🔹 UTILITY ====================

    @Transactional(readOnly = true)
    public boolean isEmailRegistered(String email) {
        return isValidEmail(email) && userRepository.existsByEmailIgnoreCase(normalizeEmail(email));
    }
}