package com.safar_zone_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PackageStatsResponse {

    private long upcoming;

    private long ongoing;

    private long completed;

    private long cancelled;

    private long total;
}
