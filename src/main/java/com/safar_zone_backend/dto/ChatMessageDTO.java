package com.safar_zone_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageDTO {

    private String conversationId;

    private String senderId;

    private String receiverId;

    private String senderRole;

    private String message;
}