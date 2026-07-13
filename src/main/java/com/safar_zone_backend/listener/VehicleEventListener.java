package com.safar_zone_backend.listener;

import com.safar_zone_backend.event.VehicleCreatedEvent;
import com.safar_zone_backend.services.AnalyticsService;
import com.safar_zone_backend.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// VehicleEventListener.java - Ab ye kaam karega!

@Component
@RequiredArgsConstructor
@Slf4j
public class VehicleEventListener {

    private final NotificationService notificationService;  // ✅ Ab exist karta hai
    private final AnalyticsService analyticsService;         // ✅ Ab exist karta hai

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVehicleCreated(VehicleCreatedEvent event) {
        try {
            log.info("🎯 Processing VehicleCreatedEvent: {} (driver: {})",
                    event.getVehicleId(), event.getDriverId());

            // ✅ Ab ye calls compile + run hongi (stub logs print honge)
            notificationService.sendVehicleAddedNotification(
                    event.getDriverId(),
                    event.getVehicleId()
            );

            analyticsService.trackVehicleCreated(
                    event.getVehicleId(),
                    event.getVehicleType().name()
            );

            log.debug("✅ Completed async tasks for vehicle: {}", event.getVehicleId());

        } catch (Exception e) {
            log.error("❌ Async processing failed for vehicle {}: {}",
                    event.getVehicleId(), e.getMessage(), e);
        }
    }
}