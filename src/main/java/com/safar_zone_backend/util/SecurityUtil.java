// src/main/java/com/safar_zone_backend/util/SecurityUtil.java
package com.safar_zone_backend.util;

import com.safar_zone_backend.entity.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * ✅ Central utility to extract authenticated user info
 * Use this in Controllers/Services instead of @AuthenticationPrincipal
 */
@Component
public class SecurityUtil {

    /**
     * ✅ Get current authenticated user's userId
     * @throws IllegalStateException if user not authenticated
     */
    public String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            throw new IllegalStateException("User not authenticated");
        }
        return ((CustomUserDetails) auth.getPrincipal()).getUserId();
    }

    /**
     * ✅ Get current user's role
     */
    public Role getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            throw new IllegalStateException("User not authenticated");
        }
        return ((CustomUserDetails) auth.getPrincipal()).getRole();
    }

    /**
     * ✅ Get full CustomUserDetails object
     */
    public CustomUserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            throw new IllegalStateException("User not authenticated");
        }
        return (CustomUserDetails) auth.getPrincipal();
    }

    /**
     * ✅ Safe getter - returns null if not authenticated (for optional auth endpoints)
     */
    public String getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /**
     * ✅ Check if current user has required role
     */
    public boolean hasRole(Role requiredRole) {
        try {
            return getCurrentUserRole() == requiredRole;
        } catch (IllegalStateException e) {
            return false;
        }
    }
}