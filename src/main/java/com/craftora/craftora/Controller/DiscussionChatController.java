package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.DiscussionRoom;
import com.craftora.craftora.Entity.User;
import com.craftora.craftora.Repository.DiscussionMessageRepository;
import com.craftora.craftora.Repository.DiscussionRoomRepository;
import com.craftora.craftora.Repository.UserRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class DiscussionChatController {

    private final DiscussionRoomRepository discussionRoomRepository;
    private final DiscussionMessageRepository discussionMessageRepository;
    private final UserRepository userRepository;

    public DiscussionChatController(
            DiscussionRoomRepository discussionRoomRepository,
            DiscussionMessageRepository discussionMessageRepository,
            UserRepository userRepository) {

        this.discussionRoomRepository = discussionRoomRepository;
        this.discussionMessageRepository = discussionMessageRepository;
        this.userRepository = userRepository;
    }

    // =========================================================
    // ADMIN CHAT PAGE
    // =========================================================

    @GetMapping("/admin/events/discussion/{discussionId}")
    public String adminDiscussion(
            @PathVariable Long discussionId,
            HttpSession session,
            Model model) {

        User user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        if (!isAdmin(user)) {
            return "redirect:/login";
        }

        DiscussionRoom room =
                discussionRoomRepository
                        .findById(discussionId)
                        .orElse(null);

        if (room == null) {
            return "redirect:/admin/events";
        }

        model.addAttribute("room", room);

        model.addAttribute(
                "messages",
                discussionMessageRepository
                        .findByDiscussionRoomDiscussionIdOrderBySentAtAsc(
                                discussionId
                        )
        );

        model.addAttribute("currentUser", user);

        // This is still needed for Admin functionality
        model.addAttribute("isAdmin", true);

        // -----------------------------------------------------
        // BACK BUTTON FOR ADMIN
        // -----------------------------------------------------

        model.addAttribute(
                "backUrl",
                "/admin/events"
        );

        model.addAttribute(
                "backTitle",
                "Back to Admin Events"
        );

        return "discussion-chat";
    }


    // =========================================================
    // ARTISAN CHAT PAGE
    // =========================================================

    @GetMapping("/artisan/events/discussion/{discussionId}")
    public String artisanDiscussion(
            @PathVariable Long discussionId,
            HttpSession session,
            Model model) {

        User user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        DiscussionRoom room =
                discussionRoomRepository
                        .findById(discussionId)
                        .orElse(null);

        if (room == null) {
            return "redirect:/artisan/events/discussions";
        }

        model.addAttribute("room", room);

        model.addAttribute(
                "messages",
                discussionMessageRepository
                        .findByDiscussionRoomDiscussionIdOrderBySentAtAsc(
                                discussionId
                        )
        );

        model.addAttribute("currentUser", user);

        // -----------------------------------------------------
        // KEEP ADMIN STATUS
        // -----------------------------------------------------

        model.addAttribute(
                "isAdmin",
                isAdmin(user)
        );

        // -----------------------------------------------------
        // BACK BUTTON FOR ARTISAN SIDE
        // -----------------------------------------------------
        //
        // IMPORTANT:
        // Even if this user is also an ADMIN,
        // they entered through the ARTISAN URL.
        //
        // Therefore Back must return to:
        //
        // /artisan/events/discussions
        //
        // -----------------------------------------------------

        model.addAttribute(
                "backUrl",
                "/artisan/events/discussions"
        );

        model.addAttribute(
                "backTitle",
                "Back to Event Discussions"
        );

        return "discussion-chat";
    }


    // =========================================================
    // GET LOGGED-IN USER
    // =========================================================

    private User getLoggedInUser(HttpSession session) {

        Object userIdObject =
                session.getAttribute("loggedInUserId");

        if (userIdObject == null) {
            return null;
        }

        Long userId;

        try {
            userId =
                    Long.valueOf(
                            userIdObject.toString()
                    );
        } catch (Exception e) {
            return null;
        }

        return userRepository
                .findById(userId)
                .orElse(null);
    }


    // =========================================================
    // CHECK ADMIN
    // =========================================================

    private boolean isAdmin(User user) {

        return user.getRoles() != null
                && user.getRoles()
                .stream()
                .anyMatch(role ->
                        role.getRoleName()
                                .equalsIgnoreCase("ADMIN"));
    }
}