package com.safar_zone_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String conversationId;

    private String senderId;

    private String receiverId;

    private String senderRole;

    @Column(columnDefinition = "TEXT")
    private String message;

    private boolean seen;

    private LocalDateTime createdAt;
}