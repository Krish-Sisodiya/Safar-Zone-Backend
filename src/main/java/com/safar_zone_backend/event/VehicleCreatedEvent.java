package com.safar_zone_backend.event;

import com.safar_zone_backend.entity.VehicleType;
import lombok.Builder;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * ✅ Event published when a new vehicle is successfully created
 * Used for async post-processing: notifications, analytics, image optimization, etc.
 */
@Getter
public class VehicleCreatedEvent extends ApplicationEvent {

    private final String vehicleId;
    private final String driverId;
    private final VehicleType vehicleType;
    private final String vehicleNumber;

    // ✅ FIXED: Renamed to avoid conflict with parent's getTimestamp()
    private final long eventTimestamp;  // ✅ Changed from 'timestamp' to 'eventTimestamp'

    @Builder
    public VehicleCreatedEvent(String vehicleId, String driverId, VehicleType vehicleType, String vehicleNumber) {
        super(vehicleId);  // source = vehicleId
        this.vehicleId = vehicleId;
        this.driverId = driverId;
        this.vehicleType = vehicleType;
        this.vehicleNumber = vehicleNumber;
        this.eventTimestamp = System.currentTimeMillis();  // ✅ Custom timestamp
    }

    // ✅ Simple constructor for quick usage
    public VehicleCreatedEvent(String vehicleId, String driverId, VehicleType vehicleType) {
        this(vehicleId, driverId, vehicleType, null);
    }

    // ✅ Optional: Helper to get parent's timestamp (Spring's event creation time)
    public long getSpringEventTimestamp() {
        return super.getTimestamp();  // ✅ Access parent's final method
    }
}