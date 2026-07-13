// 📁 src/main/java/com/safar_zone_backend/dto/VehicleCountByType.java
package com.safar_zone_backend.dto;

import com.safar_zone_backend.entity.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ✅ DTO for vehicle count grouped by type
 * Used in JPQL: SELECT new VehicleCountByType(v.type, COUNT(v))
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCountByType {
    private VehicleType type;
    private Long count;

    // ✅ Optional: Helper for frontend
    public String getTypeLabel() {
        return type != null ? type.name() : "UNKNOWN";
    }
}