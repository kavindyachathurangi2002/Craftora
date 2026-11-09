package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.DiscussionRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiscussionRoomRepository extends JpaRepository<DiscussionRoom, Long> {

    Optional<DiscussionRoom>
    findFirstByEventTypeIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
            String eventType,
            String status
    );

    List<DiscussionRoom>
    findByEventTypeIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
            String eventType,
            String status
    );
}