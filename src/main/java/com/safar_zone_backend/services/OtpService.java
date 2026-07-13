package com.safar_zone_backend.services;

import com.safar_zone_backend.entity.Otp;
import com.safar_zone_backend.entity.Role;
import com.safar_zone_backend.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OtpService {

    private final OtpRepository otpRepository;
    private final JavaMailSender mailSender;

    @Value("${app.otp.length:6}") private int otpLength;
    @Value("${app.otp.expiry-minutes:10}") private int otpExpiryMinutes;
    @Value("${app.otp.max-attempts-per-hour:5}") private int maxOtpAttemptsPerHour;
    @Value("${spring.mail.username:no-reply@safarzone.com}") private String senderEmail;
    @Value("${app.base-url:https://safarzone.com}") private String baseUrl;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern OTP_PATTERN = Pattern.compile("^\\d+$");
    private static final SecureRandom secureRandom = new SecureRandom();

    // ✅ Public record for external use (AuthService can import this)
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

        try {
            sendOtpEmail(normalizedEmail, otpCode, role);
            log.info("✅ OTP sent to {}", normalizedEmail);
        } catch (MailException e) {
            log.error("❌ Failed to send OTP email: {}", e.getMessage());
            throw new RuntimeException("Failed to send verification email. Please try again.");
        }
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

    private void sendOtpEmail(String to, String otp, Role role) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(senderEmail);
        msg.setTo(to);
        msg.setSubject("🔐 Safar Zone - Verification Code");
        msg.setText(String.format("""
            Hello!
            
            Your Safar Zone verification code (%s) is:
            
            🔑 %s
            
            Expires in %d minutes.
            
            Ignore if not requested.
            
            Safar Zone Team 🚗✈️
            %s
            """, role != null ? role.name() : "User", otp, otpExpiryMinutes, baseUrl));
        mailSender.send(msg);
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