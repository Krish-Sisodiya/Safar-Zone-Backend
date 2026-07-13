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
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String travelerId;

    private String driverId;

    private String packageId;

    private boolean active;

    private LocalDateTime createdAt;
}