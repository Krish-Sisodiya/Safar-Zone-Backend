package com.safar_zone_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartConversationDTO {

    private String travelerId;

    private String driverId;

    private String packageId;
}