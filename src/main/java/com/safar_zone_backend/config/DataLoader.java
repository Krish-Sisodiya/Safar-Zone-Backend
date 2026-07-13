package com.safar_zone_backend.config;

import com.safar_zone_backend.entity.DriverVerificationStatus;
import com.safar_zone_backend.entity.Role;
import com.safar_zone_backend.entity.User;
import com.safar_zone_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        createSuperAdmin();

    }

    private void createSuperAdmin() {

        String email = "superadmin@safarzone.com";

        if (userRepository.existsByEmail(email)) {
            log.info("✅ Super Admin already exists");
            return;
        }

        User superAdmin = User.builder()
                .name("Super Admin")
                .email(email)

                // 🔥 IMPORTANT FIX
                .phone("9999999999")

                .password(passwordEncoder.encode("Super@123"))
                .role(Role.SUPER_ADMIN)
                .isVerified(true)
                .driverVerificationStatus(
                        DriverVerificationStatus.VERIFIED
                )
                .build();

        userRepository.save(superAdmin);

        log.info("🔥 SUPER ADMIN CREATED");
    }
}