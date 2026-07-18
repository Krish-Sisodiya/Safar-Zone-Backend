package com.safar_zone_backend.services;

import com.safar_zone_backend.entity.Otp;
import com.safar_zone_backend.entity.Role;
import com.safar_zone_backend.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OtpService {

    private final OtpRepository otpRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.otp.length:6}") private int otpLength;
    @Value("${app.otp.expiry-minutes:10}") private int otpExpiryMinutes;
    @Value("${app.otp.max-attempts-per-hour:5}") private int maxOtpAttemptsPerHour;

    // Cloud security bypass variables
    @Value("${BREVO_API_KEY}") private String brevoApiKey;
    @Value("${spring.mail.username:no-reply@safarzone.com}") private String senderEmail;
    @Value("${app.base-url:https://safarzone.com}") private String baseUrl;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern OTP_PATTERN = Pattern.compile("^\\d+$");
    private static final SecureRandom secureRandom = new SecureRandom();

    // ✅ Public record for external use (AuthService compatibility)
    public record OtpVerificationResult(
            boolean success,
            String reason,
            String email
    ) {
        public static OtpVerificationResult success(String email) {
            return new OtpVerificationResult(true, "SUCCESS", email);
        }
        public static OtpVerificationResult invalid(String email) {
            return new OtpVerificationResult(false, "INVALID", email);
        }
        public static OtpVerificationResult expired(String email) {
            return new OtpVerificationResult(false, "EXPIRED", email);
        }
        public static OtpVerificationResult alreadyUsed(String email) {
            return new OtpVerificationResult(false, "ALREADY_USED", email);
        }
        public static OtpVerificationResult notFound(String email) {
            return new OtpVerificationResult(false, "NOT_FOUND", email);
        }
    }

    public void sendOtp(String email, String roleStr) {
        log.info("📤 Sending OTP to: {}", email);

        validateEmail(email);
        Role role = parseRole(roleStr);
        String normalizedEmail = normalizeEmail(email);

        checkOtpRateLimit(normalizedEmail);
        otpRepository.deleteByEmail(normalizedEmail);

        String otpCode = generateSecureOtp();

        Otp otp = Otp.builder()
                .email(normalizedEmail)
                .otpCode(otpCode)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .used(false)
                .build();
        otpRepository.save(otp);

        // ✅ Switch from SMTP to Secure Cloud REST API pipeline
        sendOtpViaBrevoApi(normalizedEmail, otpCode, role);
    }

    @Transactional(readOnly = true)
    public OtpVerificationResult verifyOtp(String email, String otpCode) {
        log.info("🔐 Verifying OTP for: {}", email);

        if (!isValidEmail(email) || !isValidOtpFormat(otpCode)) {
            return OtpVerificationResult.invalid(normalizeEmail(email));
        }

        String normalizedEmail = normalizeEmail(email);
        String normalizedOtp = otpCode.trim();

        Optional<Otp> otpOpt = otpRepository.findByEmailAndOtpCode(normalizedEmail, normalizedOtp);
        if (otpOpt.isEmpty()) {
            return OtpVerificationResult.notFound(normalizedEmail);
        }

        Otp otp = otpOpt.get();

        if (Boolean.TRUE.equals(otp.getUsed())) {
            return OtpVerificationResult.alreadyUsed(normalizedEmail);
        }

        if (otp.getExpiresAt() == null || otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            return OtpVerificationResult.expired(normalizedEmail);
        }

        otp.setUsed(true);
        otpRepository.save(otp);

        log.info("✅ OTP verified: {}", normalizedEmail);
        return OtpVerificationResult.success(normalizedEmail);
    }

    // ==================== 🔹 HELPERS ====================

    private String generateSecureOtp() {
        int max = (int) Math.pow(10, otpLength);
        return String.format("%0" + otpLength + "d", secureRandom.nextInt(max));
    }

    private void sendOtpViaBrevoApi(String to, String otp, Role role) {
        String url = "https://api.brevo.com/v3/smtp/email";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);

        String roleName = role != null ? role.name() : "User";
        String htmlMessage = String.format("""
            <h3>Hello %s!</h3>
            <p>Your Safar Zone verification code is:</p>
            <h2 style="color: #4A90E2; letter-spacing: 2px;">🔑 %s</h2>
            <p>Expires in <b>%d minutes</b>.</p>
            <p>Ignore if not requested.</p>
            <hr>
            <p style="font-size: 12px; color: #888;">Safar Zone Team 🚗✈️<br><a href="%s">%s</a></p>
            """, roleName, otp, otpExpiryMinutes, baseUrl, baseUrl);

        Map<String, Object> body = Map.of(
                "sender", Map.of("name", "Safar Zone", "email", senderEmail),
                "to", List.of(Map.of("email", to)),
                "subject", "🔐 Safar Zone - Verification Code",
                "htmlContent", htmlMessage
        );

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                log.info("✅ OTP email sent successfully via API to {}", to);
            } else {
                log.error("❌ Brevo API returned status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to send email via Brevo API");
            }
        } catch (Exception e) {
            log.error("❌ REST API Email failed: {}", e.getMessage());
            throw new RuntimeException("Failed to send verification email. Please try again.");
        }
    }

    private void validateEmail(String email) {
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    private boolean isValidEmail(String email) {
        return StringUtils.hasText(email) && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private boolean isValidOtpFormat(String otp) {
        return StringUtils.hasText(otp) && OTP_PATTERN.matcher(otp.trim()).matches()
                && otp.trim().length() == otpLength;
    }

    private Role parseRole(String roleStr) {
        if (!StringUtils.hasText(roleStr)) return Role.TRAVELER;
        try {
            return Role.valueOf(roleStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid role: {}, defaulting to TRAVELER", roleStr);
            return Role.TRAVELER;
        }
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase().trim();
    }

    private void checkOtpRateLimit(String email) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long count = otpRepository.countByEmailAndCreatedAtAfter(email, oneHourAgo);
        if (count >= maxOtpAttemptsPerHour) {
            throw new IllegalArgumentException("Too many requests. Try again after 1 hour.");
        }
    }

    @Transactional(readOnly = true)
    public boolean hasPendingOtp(String email) {
        if (!isValidEmail(email)) return false;
        String normalized = normalizeEmail(email);
        LocalDateTime now = LocalDateTime.now();
        return otpRepository.findByEmail(normalized)
                .stream().anyMatch(o -> !Boolean.TRUE.equals(o.getUsed())
                        && o.getExpiresAt() != null && o.getExpiresAt().isAfter(now));
    }

    @Transactional
    public int cleanupExpiredOtps() {
        int deleted = otpRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.debug("🧹 Cleaned {} expired OTPs", deleted);
        return deleted;
    }
}