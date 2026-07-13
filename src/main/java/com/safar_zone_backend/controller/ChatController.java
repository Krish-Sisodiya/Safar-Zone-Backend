package com.safar_zone_backend.controller;

import com.safar_zone_backend.dto.ChatMessageDTO;
import com.safar_zone_backend.entity.Message;
import com.safar_zone_backend.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    private final MessageRepository messageRepository;

    @MessageMapping("/sendMessage")
    public void sendMessage(ChatMessageDTO dto) {

        Message message =
                Message.builder()
                        .conversationId(dto.getConversationId())
                        .senderId(dto.getSenderId())
                        .receiverId(dto.getReceiverId())
                        .senderRole(dto.getSenderRole())
                        .message(dto.getMessage())
                        .seen(false)
                        .createdAt(LocalDateTime.now())
                        .build();

        Message saved =
                messageRepository.save(message);

        messagingTemplate.convertAndSend(
                "/topic/conversation/" +
                        dto.getConversationId(),
                saved
        );
    }
}