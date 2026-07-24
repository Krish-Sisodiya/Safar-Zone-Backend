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

        // ✅ 0. CRITICAL FIX: Direct bypass for CORS Preflight (OPTIONS) requests
        if ("OPTIONS".equalsIgnoreCase(requestMethod)) {
            response.setStatus(HttpServletResponse.SC_OK);
            chain.doFilter(request, response);
            return;
        }

        log.debug("🔍 Filtering request: {} {}", requestMethod, requestUri);

        // ✅ SKIP WEBSOCKET ENDPOINTS
        if (requestUri.startsWith("/chat") || requestUri.startsWith("/topic") || requestUri.startsWith("/app")) {
            chain.doFilter(request, response);
            return;
        }

        // ✅ 2. Skip public endpoints (NO auth required)
        if (isPublicEndpoint(requestUri)) {
            log.debug("✅ Public endpoint, skipping auth: {}", requestUri);
            chain.doFilter(request, response);
            return;
        }

        // ✅ 3. Extract token from Authorization header
        String token = extractToken(request);
        if (token == null) {
            log.warn("⚠️ No token found in request: {} {}", requestMethod, requestUri);
            if (!response.isCommitted()) {
                sendUnauthorized(response, "Authentication token is missing");
            }
            return;
        }

        // ✅ 4. Validate token
        JwtUtil.ValidationResult result = jwtUtil.validateToken(token);
        if (!result.isValid()) {
            sendUnauthorized(response, "Invalid or expired token: " + result.errorMessage());
            return;
        }

        // ✅ 5. Extract claims
        String userId = jwtUtil.extractUserId(token);
        String email = jwtUtil.extractEmail(token);
        String roleString = jwtUtil.extractRole(token);

        if (userId == null || userId.isBlank() || email == null || email.isBlank()) {
            sendUnauthorized(response, "Invalid token: missing user information");
            return;
        }

        // ✅ 6. Parse role
        Role role;
        try {
            role = parseRole(roleString);
        } catch (SecurityException e) {
            sendUnauthorized(response, "Invalid token: " + e.getMessage());
            return;
        }

        CustomUserDetails userDetails = new CustomUserDetails(userId, email, role);
        var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

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