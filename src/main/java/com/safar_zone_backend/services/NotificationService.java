package com.safar_zone_backend.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    /**
     * ✅ Stub: Send vehicle added notification to driver
     * TODO: Integrate with Firebase Cloud Messaging / SMS / Email
     */
    public void sendVehicleAddedNotification(String driverId, String vehicleId) {
        log.info("🔔 [STUB] Would send notification to driver {} for vehicle {}", driverId, vehicleId);

    }

    public void sendDriverWelcome(String driverId) {
        log.info("🔔 [STUB] Welcome notification sent to driver: {}", driverId);
        // 🔮 Future: Firebase Push / Twilio SMS / SendGrid Email
    }
}