package com.safar_zone_backend.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {

    private String conversationId;

    private String travelerId;

    private String travelerName;

    private String travelerImage;

    private String driverId;

    private String packageId;

    private String lastMessage;

    private String lastMessageTime;

    private boolean unread;
}