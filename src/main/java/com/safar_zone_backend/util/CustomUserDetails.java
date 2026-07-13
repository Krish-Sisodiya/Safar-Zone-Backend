package com.safar_zone_backend.util;

import com.safar_zone_backend.entity.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class CustomUserDetails implements UserDetails {

    private final String userId;
    private final String email;
    // private final String phone;
    private final Role role;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(String userId, String email, Role role) {
        this.userId = userId;
        this.email = email;
        // this.phone = phone != null ? phone : "";
        this.role = role != null ? role : Role.TRAVELER;
        this.authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + this.role.name())
        );
    }

    // ✅ Spring Security required methods
    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return null; }  // JWT auth, no password needed
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    // ✅ Helper methods for role checks in controllers
    public boolean hasRole(Role requiredRole) {
        return this.role == requiredRole;
    }
    public boolean isAdmin() { return this.role == Role.ADMIN; }
    public boolean isDriver() { return this.role == Role.DRIVER; }
    public boolean isTraveler() { return this.role == Role.TRAVELER; }
}