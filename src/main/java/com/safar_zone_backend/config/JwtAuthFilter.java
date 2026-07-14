package com.safar_zone_backend.config;

import com.safar_zone_backend.entity.Role;
import com.safar_zone_backend.util.CustomUserDetails;
import com.safar_zone_backend.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // ✅ Public endpoints (NO auth required)
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/**",
            "/api/public/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error",
            "/health",
            "/uploads/**"
    );


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String requestMethod = request.getMethod();

        // ✅ 1. DEBUG: Log incoming request
        log.debug("🔍 Filtering request: {} {}", requestMethod, requestUri);

        // ✅ SKIP WEBSOCKET ENDPOINTS
        if (
                requestUri.startsWith("/chat") ||
                        requestUri.startsWith("/topic") ||
                        requestUri.startsWith("/app")
        ) {

            log.debug("✅ WebSocket endpoint, skipping JWT filter: {}", requestUri);

            chain.doFilter(request, response);

            return;
        }

        // ✅ 2. Skip public endpoints (NO auth required) - SAFELY
        if (isPublicEndpoint(requestUri)) {
            log.debug("✅ Public endpoint, skipping auth: {}", requestUri);
            chain.doFilter(request, response);
            return; // Ekdum perfect, bina response chhede return ho jao
        }

        // ✅ 3. Extract token from Authorization header
        String token = extractToken(request);
        if (token == null) {
            log.warn("⚠️ No token found in request: {} {}", requestMethod, requestUri);
            // 🚨 DIRECT CRASH SE BACHNE KE LIYE: Phle check karo response commit toh nahi hua
            if (!response.isCommitted()) {
                sendUnauthorized(response, "Authentication token is missing");
            }
            return;
        }

        // ✅ DEBUG: Log token presence (not the token itself for security)
        log.debug("🔑 Token present in request: {} {}", requestMethod, requestUri);

        // ✅ 4. Validate token using JwtUtil.ValidationResult
        JwtUtil.ValidationResult result = jwtUtil.validateToken(token);
        if (!result.isValid()) {
            log.warn("⚠️ Invalid token: {} for URI: {} {}",
                    result.errorMessage(), requestMethod, requestUri);
            sendUnauthorized(response, "Invalid or expired token: " + result.errorMessage());
            return;
        }

        // ✅ DEBUG: Token is valid, extract claims
        log.debug("✅ Token validated successfully for URI: {} {}", requestMethod, requestUri);

        // ✅ 5. Extract claims from validated token WITH DETAILED LOGGING
        String userId = jwtUtil.extractUserId(token);
        String email = jwtUtil.extractEmail(token);
        String roleString = jwtUtil.extractRole(token);

        // ✅ DEBUG: Log extracted claims (CRITICAL for debugging 403 errors)
        log.debug("📋 Extracted token claims:");
        log.debug("   • userId: '{}'", userId != null ? userId : "NULL");
        log.debug("   • email: '{}'", email != null ? email : "NULL");
        log.debug("   • role (raw string): '{}'", roleString != null ? roleString : "NULL");

        // ✅ 6. Validate required claims (userId & email are mandatory)
        if (userId == null || userId.isBlank() || email == null || email.isBlank()) {
            log.error("🚫 Token missing required claims (userId/email) for URI: {} {}",
                    requestMethod, requestUri);
            sendUnauthorized(response, "Invalid token: missing user information");
            return;
        }

        // ✅ 7. Parse role - REJECT invalid instead of defaulting (SECURITY FIX)
        Role role;
        try {
            role = parseRole(roleString);
            log.debug("✅ Role parsed successfully: {} (from: '{}')", role, roleString);
        } catch (SecurityException e) {
            log.error("🚫 Role parsing failed for user {} (email: {}): {}",
                    userId, email, e.getMessage());
            sendUnauthorized(response, "Invalid token: " + e.getMessage());
            return;
        }

        // ✅ 8. Create CustomUserDetails with parsed Role
        CustomUserDetails userDetails = new CustomUserDetails(userId, email, role);
        log.debug("👤 Created CustomUserDetails: userId={}, email={}, role={}",
                userId, email, role);

        // ✅ 9. Create Spring Security authorities - CRITICAL: Must match hasRole() format
        // Spring Security's hasRole("DRIVER") expects authority: "ROLE_DRIVER"
        var authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
        log.debug("🔐 Created authority: ROLE_{} for user {}", role.name(), userId);

        // ✅ 10. Create authentication object and set in SecurityContext
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ✅ FINAL: Log successful authentication
        log.info("✅ Authenticated: {} (email: {}, role: {}) for {} {}",
                userId, email, role, requestMethod, requestUri);

        // ✅ Proceed with the request
        chain.doFilter(request, response);
    }

    // ==================== 🔹 HELPER METHODS ====================

    private boolean isPublicEndpoint(String uri) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(p -> pathMatcher.match(p, uri));
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
            return null;
        }
        String token = authHeader.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    // 📁 src/main/java/com/safar_zone_backend/config/JwtAuthFilter.java

    /**
     * ✅ Parse role string → Role enum
     * 🎯 KEY FIX: REJECT invalid/null roles instead of defaulting to TRAVELER
     */
    private Role parseRole(String roleString) {
        // ✅ REJECT null/blank roles - security fix!
        if (roleString == null || roleString.isBlank()) {
            log.error("🚫 Token missing 'role' claim - rejecting authentication");
            throw new SecurityException("Token missing required role claim");
        }

        try {
            // ✅ Convert to uppercase and parse enum
            String normalizedRole = roleString.trim().toUpperCase();
            Role role = Role.valueOf(normalizedRole);
            log.debug("✅ Successfully parsed role: {} (from: '{}')", role, roleString);
            return role;

        } catch (IllegalArgumentException e) {
            // ✅ Log available roles for debugging
            String availableRoles = java.util.Arrays.stream(Role.values())
                    .map(Role::name)
                    .collect(java.util.stream.Collectors.joining(", "));

            log.error("🚫 Invalid role '{}' in token. Available roles: {}",
                    roleString, availableRoles);
            throw new SecurityException("Invalid role in token: '" + roleString +
                    "'. Expected one of: " + availableRoles);
        }
    }
    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        sendError(response, HttpServletResponse.SC_UNAUTHORIZED, message);
    }

    private void sendForbidden(HttpServletResponse response, String message) throws IOException {
        sendError(response, HttpServletResponse.SC_FORBIDDEN, message);
    }

    private void sendError(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String json = String.format(
                "{\"success\":false,\"error\":{\"message\":\"%s\",\"code\":%d}}",
                message.replace("\"", "\\\""),
                statusCode
        );
        response.getWriter().write(json);
        response.getWriter().flush();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }



}