package com.safar_zone_backend.services;

import com.safar_zone_backend.entity.VehicleType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
@Slf4j
public class AnalyticsService {

    /**
     * ✅ Stub: Track custom analytics event
     * TODO: Integrate with Google Analytics / Mixpanel / Custom DB
     */
    public void track(String eventName, Map<String, Object> properties) {
        log.info("📊 [STUB] Tracking event: {} with properties: {}", eventName, properties);
        // Future implementation:
        // 1. Format event for your analytics provider
        // 2. Send via HTTP client (async)
        // 3. Handle retries + fallback logging
    }

    /**
     * ✅ Convenience method for vehicle creation
     */
    public void trackVehicleCreated(String vehicleId, String vehicleType) {
        track("vehicle_created", Map.of(
                "vehicleId", vehicleId,
                "vehicleType", vehicleType
        ));
    }


    public void trackVehicleCreated(String vehicleId, VehicleType type) {
        log.info("📊 [STUB] Tracked vehicle creation | ID: {} | Type: {}", vehicleId, type);
        // 🔮 Future: Google Analytics / Mixpanel / Custom Metrics DB
    }
}