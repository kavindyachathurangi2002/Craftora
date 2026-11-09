package com.craftora.craftora.Controller;

import com.craftora.craftora.DTO.DiscussionChatMessage;
import com.craftora.craftora.Entity.DiscussionMessage;
import com.craftora.craftora.Entity.DiscussionRoom;
import com.craftora.craftora.Entity.User;
import com.craftora.craftora.Repository.DiscussionMessageRepository;
import com.craftora.craftora.Repository.DiscussionRoomRepository;
import com.craftora.craftora.Repository.UserRepository;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class DiscussionWebSocketController {

    private final DiscussionRoomRepository discussionRoomRepository;
    private final DiscussionMessageRepository discussionMessageRepository;
    private final UserRepository userRepository;

    public DiscussionWebSocketController(
            DiscussionRoomRepository discussionRoomRepository,
            DiscussionMessageRepository discussionMessageRepository,
            UserRepository userRepository) {

        this.discussionRoomRepository = discussionRoomRepository;
        this.discussionMessageRepository = discussionMessageRepository;
        this.userRepository = userRepository;
    }

    // =========================================================
    // SEND LIVE CHAT MESSAGE
    // =========================================================

    @MessageMapping("/discussion/{discussionId}/send")
    @SendTo("/topic/discussion/{discussionId}")
    public DiscussionChatMessage sendMessage(
            @DestinationVariable Long discussionId,
            DiscussionChatMessage chatMessage) {

        // -----------------------------------------------------
        // Find discussion room
        // -----------------------------------------------------

        DiscussionRoom discussionRoom =
                discussionRoomRepository
                        .findById(discussionId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Discussion room not found."
                                )
                        );

        // -----------------------------------------------------
        // IMPORTANT:
        // Do NOT allow messages in a CLOSED discussion
        // -----------------------------------------------------

        if (!"OPEN".equalsIgnoreCase(
                discussionRoom.getStatus())) {

            throw new RuntimeException(
                    "This discussion has been closed. "
                    + "New messages are not allowed."
            );
        }

        // -----------------------------------------------------
        // Validate message
        // -----------------------------------------------------

        if (chatMessage.getMessageText() == null
                || chatMessage.getMessageText()
                .trim()
                .isEmpty()) {

            throw new RuntimeException(
                    "Message cannot be empty."
            );
        }

        // -----------------------------------------------------
        // Find sender
        // -----------------------------------------------------

        if (chatMessage.getSenderId() == null) {

            throw new RuntimeException(
                    "Sender not found."
            );
        }

        User sender =
                userRepository
                        .findById(chatMessage.getSenderId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found."
                                )
                        );

        // -----------------------------------------------------
        // Save message to database
        // -----------------------------------------------------

        DiscussionMessage message =
                new DiscussionMessage();

        message.setDiscussionRoom(discussionRoom);

        message.setSender(sender);

        message.setMessageText(
                chatMessage.getMessageText().trim()
        );

        message.setSentAt(
                LocalDateTime.now()
        );

        discussionMessageRepository.save(message);

        // -----------------------------------------------------
        // Send message to all connected users
        // -----------------------------------------------------

        DiscussionChatMessage response =
                new DiscussionChatMessage();

        response.setDiscussionId(
                discussionId
        );

        response.setSenderId(
                sender.getUserId()
        );

        response.setSenderName(
                sender.getFullName()
        );

        response.setMessageText(
                message.getMessageText()
        );

        return response;
    }
}