package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.DiscussionMessage;
import com.craftora.craftora.Entity.DiscussionRoom;
import com.craftora.craftora.Entity.EventRequest;
import com.craftora.craftora.Entity.User;
import com.craftora.craftora.Repository.ArtisanRepository;
import com.craftora.craftora.Repository.DiscussionMessageRepository;
import com.craftora.craftora.Repository.DiscussionRoomRepository;
import com.craftora.craftora.Repository.EventRequestRepository;
import com.craftora.craftora.Repository.UserRepository;
import com.craftora.craftora.Service.DiscussionRoomService;
import com.craftora.craftora.Service.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

import java.util.List;

@Controller
public class AdminDiscussionController {

    private final DiscussionRoomService discussionRoomService;
    private final DiscussionRoomRepository discussionRoomRepository;
    private final DiscussionMessageRepository discussionMessageRepository;
    private final EventRequestRepository eventRequestRepository;
    private final ArtisanRepository artisanRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public AdminDiscussionController(
            DiscussionRoomService discussionRoomService,
            DiscussionRoomRepository discussionRoomRepository,
            DiscussionMessageRepository discussionMessageRepository,
            EventRequestRepository eventRequestRepository,
            ArtisanRepository artisanRepository,
            UserRepository userRepository,
            EmailService emailService) {

        this.discussionRoomService = discussionRoomService;
        this.discussionRoomRepository = discussionRoomRepository;
        this.discussionMessageRepository = discussionMessageRepository;
        this.eventRequestRepository = eventRequestRepository;
        this.artisanRepository = artisanRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    // =========================================================
    // OPEN DISCUSSION
    // =========================================================

    @PostMapping("/admin/events/discussion/{eventType}/open")
    public String openDiscussion(
            @PathVariable String eventType,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User admin = getLoggedInAdmin(session);

        if (admin == null) {
            return "redirect:/login";
        }

        String normalizedEventType =
                eventType.toUpperCase();

        if (!isValidEventType(normalizedEventType)) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Invalid event type."
            );

            return "redirect:/admin/events";
        }

        // -----------------------------------------------------
        // Check whether an OPEN discussion already exists
        // -----------------------------------------------------

        boolean alreadyOpen =
                discussionRoomService
                        .getOpenRoom(normalizedEventType)
                        .isPresent();

        DiscussionRoom room =
                discussionRoomService
                        .createRoom(normalizedEventType);

        // -----------------------------------------------------
        // Only perform these actions for a NEW discussion
        // -----------------------------------------------------

        if (!alreadyOpen) {

            // -------------------------------------------------
            // Change PENDING requests to DISCUSSION_OPEN
            // -------------------------------------------------

            List<EventRequest> pendingRequests =
                    eventRequestRepository
                            .findByEventTypeAndStatusIgnoreCaseOrderByCreatedAtDesc(
                                    normalizedEventType,
                                    "PENDING"
                            );

            for (EventRequest request : pendingRequests) {
                request.setStatus("DISCUSSION_OPEN");
            }

            if (!pendingRequests.isEmpty()) {
                eventRequestRepository.saveAll(pendingRequests);
            }

            // -------------------------------------------------
            // Create Admin's first message
            // -------------------------------------------------

            String firstMessage =
                    createFirstAdminMessage(normalizedEventType);

            List<DiscussionMessage> existingMessages =
                    discussionMessageRepository
                            .findByDiscussionRoomDiscussionIdOrderBySentAtAsc(
                                    room.getDiscussionId()
                            );

            if (existingMessages.isEmpty()) {

                discussionRoomService
                        .createFirstAdminMessage(
                                room,
                                admin,
                                firstMessage
                        );
            }

            // -------------------------------------------------
            // Send invitation email to ALL APPROVED artisans
            // -------------------------------------------------

            String chatLink =
                    "http://localhost:8080/artisan/events/discussion/"
                            + room.getDiscussionId();

            artisanRepository
                    .findByStatusIgnoreCase("APPROVED")
                    .forEach(artisan -> {

                        if (artisan.getUser() == null) {
                            return;
                        }

                        String email =
                                artisan.getUser().getEmail();

                        String name =
                                artisan.getUser().getFullName();

                        if (email == null
                                || email.trim().isEmpty()) {
                            return;
                        }

                        emailService
                                .sendEventDiscussionInvitationEmail(
                                        email,
                                        name,
                                        normalizedEventType,
                                        chatLink
                                );
                    });
        }

        // -----------------------------------------------------
        // Directly open the live chat
        // -----------------------------------------------------

        return "redirect:/admin/events/discussion/"
                + room.getDiscussionId();
    }

    // =========================================================
    // CLOSE DISCUSSION
    // =========================================================

    @PostMapping("/admin/events/discussion/{discussionId}/close")
    public String closeDiscussion(
            @PathVariable Long discussionId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User admin = getLoggedInAdmin(session);

        if (admin == null) {
            return "redirect:/login";
        }

        DiscussionRoom room =
                discussionRoomRepository
                        .findById(discussionId)
                        .orElse(null);

        if (room == null) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Discussion room not found."
            );

            return "redirect:/admin/events";
        }

        if (!"OPEN".equalsIgnoreCase(room.getStatus())) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This discussion is already closed."
            );

            return "redirect:/admin/events/discussion/"
                    + discussionId;
        }

        // -----------------------------------------------------
        // Close the discussion room
        // -----------------------------------------------------

        discussionRoomService.closeRoom(room);

        // -----------------------------------------------------
        // IMPORTANT:
        // Requests involved in this discussion become
        // DISMISSED when the discussion is closed.
        //
        // DISMISSED means the artisan is now allowed to
        // submit another request for the same event type.
        // -----------------------------------------------------

        List<EventRequest> discussionRequests =
        eventRequestRepository
                .findByEventTypeAndStatusIgnoreCaseOrderByCreatedAtDesc(
                        room.getEventType(),
                        "DISCUSSION_OPEN"
                );

for (EventRequest request : discussionRequests) {
    request.setStatus("DISCUSSION_CLOSED");
}

if (!discussionRequests.isEmpty()) {
    eventRequestRepository.saveAll(discussionRequests);
}

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Discussion closed successfully."
        );

        return "redirect:/admin/events/discussion/"
                + discussionId;
    }

    // =========================================================
    // FIRST ADMIN MESSAGE
    // =========================================================

    private String createFirstAdminMessage(String eventType) {

        if ("WORKSHOP".equalsIgnoreCase(eventType)) {

            return """
                    Hello everyone! 👋

                    We have received several requests from our artisans to organize a Workshop on Craftora.

                    We have opened this discussion to hear your ideas and opinions before planning the event.

                    What do you think about organizing this workshop? What ideas or suggestions do you have regarding the workshop?

                    Please feel free to share your thoughts, suggestions, and ideas. Your input will help us plan an event that is valuable and enjoyable for our Craftora community.
                    """;

        } else if ("EXHIBITION".equalsIgnoreCase(eventType)) {

            return """
                    Hello everyone! 👋

                    We have received several requests from our artisans to organize an Exhibition on Craftora.

                    We have opened this discussion to hear your ideas and opinions before planning the event.

                    What do you think about organizing this exhibition? What ideas or suggestions do you have regarding the exhibition?

                    Please feel free to share your thoughts, suggestions, and ideas. Your input will help us plan an event that is valuable and enjoyable for our Craftora community.
                    """;

        } else {

            return """
                    Hello everyone! 👋

                    We have received several requests from our artisans to organize a Competition on Craftora.

                    We have opened this discussion to hear your ideas and opinions before planning the event.

                    What do you think about organizing this competition? What ideas or suggestions do you have regarding the competition?

                    Please feel free to share your thoughts, suggestions, and ideas. Your input will help us plan an event that is valuable and enjoyable for our Craftora community.
                    """;
        }
    }

    // =========================================================
    // VALIDATE EVENT TYPE
    // =========================================================

    private boolean isValidEventType(String eventType) {

        return "WORKSHOP".equals(eventType)
                || "EXHIBITION".equals(eventType)
                || "COMPETITION".equals(eventType);
    }

    // =========================================================
    // GET LOGGED-IN ADMIN
    // =========================================================

    private User getLoggedInAdmin(HttpSession session) {

        Object userIdObject =
                session.getAttribute("loggedInUserId");

        if (userIdObject == null) {
            return null;
        }

        Long userId;

        try {
            userId = Long.valueOf(userIdObject.toString());
        } catch (Exception e) {
            return null;
        }

        User user =
                userRepository.findById(userId).orElse(null);

        if (user == null) {
            return null;
        }

        boolean isAdmin =
                user.getRoles() != null
                        && user.getRoles()
                        .stream()
                        .anyMatch(role ->
                                role.getRoleName()
                                        .equalsIgnoreCase("ADMIN"));

        return isAdmin ? user : null;
    }

    // =========================================================
// VIEW PREVIOUS DISCUSSIONS
// =========================================================

@GetMapping("/admin/events/discussions/previous/{eventType}")
public String viewPreviousDiscussions(
        @PathVariable String eventType,
        HttpSession session,
        Model model) {

    User user = getLoggedInAdmin(session);

if (user == null) {
    return "redirect:/login";
}

    String normalizedEventType =
            eventType == null
                    ? ""
                    : eventType.trim().toUpperCase();

    if (!normalizedEventType.equals("WORKSHOP")
            && !normalizedEventType.equals("EXHIBITION")
            && !normalizedEventType.equals("COMPETITION")) {

        return "redirect:/admin/events";
    }

    List<DiscussionRoom> previousDiscussions =
            discussionRoomRepository
                    .findByEventTypeIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
                            normalizedEventType,
                            "CLOSED"
                    );

    model.addAttribute(
            "eventType",
            normalizedEventType
    );

    model.addAttribute(
            "previousDiscussions",
            previousDiscussions
    );

    return "admin-previous-discussions";
}
}