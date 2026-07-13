package com.safar_zone_backend.repository;

import com.safar_zone_backend.entity.Message;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository
        extends JpaRepository<Message, Long> {

    List<Message>
    findByConversationIdOrderByCreatedAtAsc(
            String conversationId
    );

    List<Message>
    findByConversationIdOrderByCreatedAtDesc(
            String conversationId
    );

    @Modifying
    @Query("""
UPDATE Message m
SET m.seen = true
WHERE m.conversationId = :conversationId
AND m.receiverId = :receiverId
""")
    void markAsSeen(
            @Param("conversationId")
            String conversationId,

            @Param("receiverId")
            String receiverId
    );
}