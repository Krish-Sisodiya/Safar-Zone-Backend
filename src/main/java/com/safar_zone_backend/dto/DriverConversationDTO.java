package com.safar_zone_backend.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverConversationDTO {

    private String bookingId;

    private String packageId;

    private String userId;

    private String userName;

    private String lastMessage;

    private String time;

    private boolean unread;
}