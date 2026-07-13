package com.safar_zone_backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

/**
 * ✅ JwtUtil - FIXED & PRODUCTION READY
 *
 * 🔐 Features:
 * - Proper JJWT 0.12+ API usage
 * - Cached signing key (thread-safe)
 * - Secret key validation (min 32 bytes for HS256)
 * - Null-safe claim extraction
 * - Detailed token validation errors
 * - Standard claims (iss, aud) support
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.jwt.issuer:safar-zone-api}")
    private String issuer;

    @Value("${app.jwt.audience:safar-zone-client}")
    private String audience;

    // ✅ Cached signing key - initialized once, thread-safe
    private SecretKey signingKey;

    /**
     * ✅ Initialize signing key with validation
     * Called automatically by Spring after dependency injection
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("🔐 Initializing JwtUtil...");

        // ✅ Validate & decode secret
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret cannot be empty");
        }

        // ✅ Handle both raw string and base64-encoded secrets
        byte[] keyBytes;
        if (secret.startsWith("base64:")) {
            // Decode base64: base64:SGVsbG9Xb3JsZDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=
            String base64Secret = secret.substring(7);
            keyBytes = Base64.getDecoder().decode(base64Secret);
        } else {
            // Raw string - convert to bytes
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        // ✅ HS256 requires minimum 256 bits = 32 bytes
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    String.format("JWT secret key is too short! Minimum 32 bytes required for HS256. Got: %d bytes",
                            keyBytes.length)
            );
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("✅ JwtUtil initialized with {}-bit key", keyBytes.length * 8);
    }

    /**
     * ✅ Generate JWT token with standard + custom claims
     */
    /**
     * ✅ Generate JWT token with ALL required claims for frontend filtering
     * Claims included: userId, email, phone, role + standard JWT claims
     */
    public String generateToken(String userId, String email, String phone, String role) {
        // ✅ Validate required fields (phone can be empty string but not null for consistency)
        if (userId == null || email == null || role == null) {
            throw new IllegalArgumentException("userId, email, and role are required");
        }
        // ✅ Handle null phone safely - convert to empty string
        String phoneClaim = phone != null ? phone : "";

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        log.debug("🔑 Generating token for user: {} (email: {}, role: {})", userId, email, role);

        return Jwts.builder()
                // ✅ Standard JWT claims
                .subject(email)                    // sub: email (used as username)
                .issuer(issuer)                    // iss: safar-zone-api
                .audience().add(audience).and()    // aud: safar-zone-client
                .issuedAt(now)                     // iat: issued timestamp
                .expiration(expiry)                // exp: expiry timestamp
                .id(java.util.UUID.randomUUID().toString())  // jti: unique token ID

                // ✅ Custom claims for frontend data filtering
                .claim("userId", userId)           // ✅ Filter API calls by user ID
                .claim("email", email)             // ✅ Display user email
                .claim("phone", phoneClaim)        // ✅ Contact/verification (NEW)
                .claim("role", role)               // ✅ Conditional UI rendering

                // ✅ Sign with cached HMAC-SHA256 key
                .signWith(signingKey)
                .compact();
    }

    /**
     * ✅ Extract all claims from token (with error handling)
     */
    public Claims extractClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtException("Token cannot be empty");
        }

        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)              // ✅ Validate issuer
                    .requireAudience(audience)          // ✅ Validate audience
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.warn("⚠️ Failed to parse token: {}", e.getMessage());
            throw e;  // Re-throw for caller to handle
        }
    }

    /**
     * ✅ Safe claim extraction - returns null if claim doesn't exist
     */
    public String extractEmail(String token) {
        try {
            return extractClaims(token).getSubject();
        } catch (JwtException e) {
            return null;
        }
    }

    public String extractRole(String token) {
        try {
            return extractClaims(token).get("role", String.class);
        } catch (JwtException | ClassCastException e) {
            return null;
        }
    }

    public String extractUserId(String token) {
        try {
            return extractClaims(token).get("userId", String.class);
        } catch (JwtException | ClassCastException e) {
            return null;
        }
    }

    /**
     * ✅ Enhanced token validation with error details
     * @return ValidationResult object instead of just boolean
     */
    public ValidationResult validateToken(String token) {
        if (token == null || token.isBlank()) {
            return ValidationResult.invalid("Token is empty");
        }

        try {
            Claims claims = extractClaims(token);

            // ✅ Check expiration (extra safety, though JJWT already checks)
            Date expiration = claims.getExpiration();
            if (expiration != null && expiration.before(new Date())) {
                return ValidationResult.invalid("Token has expired");
            }

            // ✅ Check if subject (email) exists
            if (claims.getSubject() == null) {
                return ValidationResult.invalid("Token missing subject (email)");
            }

            return ValidationResult.valid(claims);

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.debug("⏰ Token expired: {}", e.getMessage());
            return ValidationResult.invalid("Token has expired");

        } catch (io.jsonwebtoken.SignatureException e) {
            log.warn("🔐 Invalid token signature: {}", e.getMessage());
            return ValidationResult.invalid("Invalid token signature");

        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.warn("🔧 Malformed token: {}", e.getMessage());
            return ValidationResult.invalid("Malformed token");

        } catch (JwtException | IllegalArgumentException e) {
            log.warn("❌ Token validation failed: {}", e.getMessage());
            return ValidationResult.invalid("Token validation failed: " + e.getMessage());
        }
    }

    /**
     * ✅ Simple boolean check (backward compatibility)
     */
    public boolean isValidToken(String token) {
        return validateToken(token).isValid();
    }

    // ==================== HELPER: Validation Result ====================

    /**
     * ✅ Immutable result object for token validation
     */
    public record ValidationResult(
            boolean isValid,
            String errorMessage,
            Claims claims
    ) {
        public static ValidationResult valid(Claims claims) {
            return new ValidationResult(true, null, claims);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message, null);
        }
    }

    // ==================== DEBUG/ADMIN UTILS ====================

    /**
     * ✅ Decode token without validation (for debugging ONLY)
     * ⚠️ Never use this in production auth flow!
     */
    public Claims decodeTokenUnsafe(String token) {
        if (token == null) return null;
        try {
            // Parse without signature verification - FOR DEBUGGING ONLY
            return Jwts.parser()
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("🔍 Unsafe decode failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * ✅ Get token metadata for logging (without exposing sensitive data)
     */
    public TokenMetadata getTokenMetadata(String token) {
        try {
            Claims claims = extractClaims(token);
            return new TokenMetadata(
                    claims.getSubject(),  // email
                    claims.get("userId", String.class),
                    claims.get("role", String.class),
                    claims.getIssuedAt(),
                    claims.getExpiration(),
                    claims.getId()  // jti
            );
        } catch (Exception e) {
            return null;
        }
    }

    public record TokenMetadata(
            String email,
            String userId,
            String role,
            Date issuedAt,
            Date expiresAt,
            String tokenId
    ) {
        public boolean isExpired() {
            return expiresAt != null && expiresAt.before(new Date());
        }

        public long getRemainingTimeMs() {
            if (expiresAt == null) return 0;
            return Math.max(0, expiresAt.getTime() - System.currentTimeMillis());
        }
    }
}