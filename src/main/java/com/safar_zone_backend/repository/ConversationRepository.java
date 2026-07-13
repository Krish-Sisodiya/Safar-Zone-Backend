package com.safar_zone_backend.repository;

import com.safar_zone_backend.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository
        extends JpaRepository<Conversation, String> {

    Optional<Conversation>
    findByTravelerIdAndDriverIdAndPackageId(
            String travelerId,
            String driverId,
            String packageId
    );

    List<Conversation>
    findByDriverIdOrderByCreatedAtDesc(
            String driverId
    );

    List<Conversation>
    findByTravelerIdOrderByCreatedAtDesc(
            String travelerId
    );


}