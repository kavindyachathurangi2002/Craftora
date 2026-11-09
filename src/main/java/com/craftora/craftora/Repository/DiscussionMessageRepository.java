package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.DiscussionMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscussionMessageRepository
        extends JpaRepository<DiscussionMessage, Long> {

    List<DiscussionMessage>
    findByDiscussionRoomDiscussionIdOrderBySentAtAsc(
            Long discussionId
    );
}