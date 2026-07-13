package com.safar_zone_backend.controller;

import com.safar_zone_backend.dto.*;
import com.safar_zone_backend.entity.*;
import com.safar_zone_backend.repository.*;
import com.safar_zone_backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatRestController {

    private final ConversationRepository conversationRepository;

    private final MessageRepository messageRepository;

    private final UserRepository userRepository;

    private final SecurityUtil securityUtil;

    // =====================================================
    // START CONVERSATION
    // =====================================================

    @PostMapping("/conversation/start")
    public Conversation startConversation(
            @RequestBody StartConversationDTO dto
    ) {

        return conversationRepository
                .findByTravelerIdAndDriverIdAndPackageId(
                        dto.getTravelerId(),
                        dto.getDriverId(),
                        dto.getPackageId()
                )
                .orElseGet(() -> {

                    Conversation conversation =
                            Conversation.builder()
                                    .travelerId(dto.getTravelerId())
                                    .driverId(dto.getDriverId())
                                    .packageId(dto.getPackageId())
                                    .active(true)
                                    .createdAt(java.time.LocalDateTime.now())
                                    .build();

                    return conversationRepository
                            .save(conversation);
                });
    }

    // =====================================================
    // GET CHAT MESSAGES
    // =====================================================

    @GetMapping("/messages/{conversationId}")
    public List<Message> getMessages(
            @PathVariable String conversationId
    ) {

        String currentUserId =
                securityUtil.getCurrentUserId();

        Conversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Conversation not found"
                                ));

        // SECURITY

        if (
                !conversation.getTravelerId()
                        .equals(currentUserId)

                        &&

                        !conversation.getDriverId()
                                .equals(currentUserId)
        ) {

            throw new RuntimeException(
                    "Access denied"
            );
        }

        return messageRepository
                .findByConversationIdOrderByCreatedAtAsc(
                        conversationId
                );
    }

    // =====================================================
    // DRIVER CONVERSATIONS
    // =====================================================

    @GetMapping("/driver/conversations")
    public List<ConversationDTO>
    driverConversations() {

        String driverId =
                securityUtil.getCurrentUserId();

        List<Conversation> conversations =
                conversationRepository
                        .findByDriverIdOrderByCreatedAtDesc(
                                driverId
                        );

        List<ConversationDTO> result =
                new ArrayList<>();

        for (Conversation conversation : conversations) {

            User traveler =
                    userRepository
                            .findById(
                                    conversation.getTravelerId()
                            )
                            .orElse(null);

            List<Message> messages =
                    messageRepository
                            .findByConversationIdOrderByCreatedAtDesc(
                                    conversation.getId()
                            );

            Message latest =
                    messages.isEmpty()
                            ? null
                            : messages.get(0);

            result.add(
                    ConversationDTO.builder()
                            .conversationId(
                                    conversation.getId()
                            )

                            .travelerId(
                                    conversation.getTravelerId()
                            )

                            .travelerName(
                                    traveler != null
                                            ? traveler.getName()
                                            : "Traveler"
                            )

                            .driverId(
                                    conversation.getDriverId()
                            )

                            .packageId(
                                    conversation.getPackageId()
                            )

                            .lastMessage(
                                    latest != null
                                            ? latest.getMessage()
                                            : ""
                            )

                            .lastMessageTime(
                                    latest != null
                                            ? latest.getCreatedAt()
                                            .format(
                                                    DateTimeFormatter.ofPattern(
                                                            "hh:mm a"
                                                    )
                                            )
                                            : ""
                            )

                            .unread(
                                    latest != null
                                            &&
                                            !latest.isSeen()
                                            &&
                                            latest.getReceiverId() != null
                                            &&
                                            latest.getReceiverId()
                                                    .equals(driverId)
                            )

                            .build()
            );
        }

        return result;
    }

    // =====================================================
    // MARK AS SEEN
    // =====================================================

    @Transactional
    @PutMapping("/seen/{conversationId}")
    public void markSeen(
            @PathVariable String conversationId
    ) {

        String userId =
                securityUtil.getCurrentUserId();

        messageRepository.markAsSeen(
                conversationId,
                userId
        );
    }


    @GetMapping("/traveler/conversations")
    public List<ConversationDTO>
    travelerConversations() {

        String travelerId =
                securityUtil.getCurrentUserId();

        List<Conversation> conversations =
                conversationRepository
                        .findByTravelerIdOrderByCreatedAtDesc(
                                travelerId
                        );

        List<ConversationDTO> result =
                new ArrayList<>();

        for (Conversation conversation : conversations) {

            List<Message> messages =
                    messageRepository
                            .findByConversationIdOrderByCreatedAtDesc(
                                    conversation.getId()
                            );

            Message latest =
                    messages.isEmpty()
                            ? null
                            : messages.get(0);

            result.add(

                    ConversationDTO.builder()

                            .conversationId(
                                    conversation.getId()
                            )

                            .travelerId(
                                    conversation.getTravelerId()
                            )

                            .driverId(
                                    conversation.getDriverId()
                            )

                            .packageId(
                                    conversation.getPackageId()
                            )

                            .lastMessage(
                                    latest != null
                                            ? latest.getMessage()
                                            : ""
                            )

                            .lastMessageTime(
                                    latest != null
                                            ? latest.getCreatedAt()
                                            .format(
                                                    DateTimeFormatter.ofPattern(
                                                            "hh:mm a"
                                                    )
                                            )
                                            : ""
                            )

                            .unread(
                                    latest != null
                                            &&
                                            !latest.isSeen()
                                            &&
                                            latest.getReceiverId() != null
                                            &&
                                            latest.getReceiverId()
                                                    .equals(travelerId)
                            )

                            .build()
            );
        }

        return result;
    }
}