package com.safar_zone_backend.services;

import com.safar_zone_backend.event.VehicleCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehiclePostCreateService {

    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVehicleCreated(VehicleCreatedEvent event) {
        try {
            // ✅ These run AFTER transaction commits, in background thread
            notificationService.sendDriverWelcome(event.getDriverId());
            analyticsService.trackVehicleCreated(event.getVehicleId(), event.getVehicleType());
            log.debug("✅ Post-create tasks completed for vehicle: {}", event.getVehicleId());
        } catch (Exception e) {
            log.error("❌ Post-create task failed (non-blocking)", e);
            // Don't throw - main transaction already committed
        }
    }
}
