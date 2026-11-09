package com.craftora.craftora.Service;

import com.craftora.craftora.Entity.DiscussionMessage;
import com.craftora.craftora.Entity.DiscussionRoom;
import com.craftora.craftora.Entity.User;
import com.craftora.craftora.Repository.DiscussionMessageRepository;
import com.craftora.craftora.Repository.DiscussionRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class DiscussionRoomService {

    private final DiscussionRoomRepository discussionRoomRepository;
    private final DiscussionMessageRepository discussionMessageRepository;

    public DiscussionRoomService(
            DiscussionRoomRepository discussionRoomRepository,
            DiscussionMessageRepository discussionMessageRepository) {

        this.discussionRoomRepository = discussionRoomRepository;
        this.discussionMessageRepository = discussionMessageRepository;
    }

    public Optional<DiscussionRoom> getOpenRoom(String eventType) {

        return discussionRoomRepository
                .findFirstByEventTypeIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
                        eventType,
                        "OPEN"
                );
    }

    @Transactional
    public DiscussionRoom createRoom(String eventType) {

        Optional<DiscussionRoom> existingRoom =
                getOpenRoom(eventType);

        if (existingRoom.isPresent()) {
            return existingRoom.get();
        }

        DiscussionRoom room = new DiscussionRoom();

        room.setEventType(eventType.toUpperCase());
        room.setStatus("OPEN");
        room.setCreatedAt(LocalDateTime.now());

        return discussionRoomRepository.save(room);
    }

    @Transactional
    public DiscussionMessage createFirstAdminMessage(
            DiscussionRoom room,
            User admin,
            String messageText) {

        DiscussionMessage message = new DiscussionMessage();

        message.setDiscussionRoom(room);
        message.setSender(admin);
        message.setMessageText(messageText);
        message.setSentAt(LocalDateTime.now());

        return discussionMessageRepository.save(message);
    }

    @Transactional
    public DiscussionRoom closeRoom(DiscussionRoom room) {

        room.setStatus("CLOSED");

        return discussionRoomRepository.save(room);
    }
}