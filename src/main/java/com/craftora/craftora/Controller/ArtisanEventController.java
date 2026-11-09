package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Entity.DiscussionRoom;
import com.craftora.craftora.Repository.ArtisanRepository;
import com.craftora.craftora.Repository.DiscussionRoomRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ArtisanEventController {

    private final ArtisanRepository artisanRepository;
    private final DiscussionRoomRepository discussionRoomRepository;

    public ArtisanEventController(
            ArtisanRepository artisanRepository,
            DiscussionRoomRepository discussionRoomRepository) {

        this.artisanRepository = artisanRepository;
        this.discussionRoomRepository = discussionRoomRepository;
    }

    // =====================================================
    // ARTISAN EVENT DISCUSSIONS
    // =====================================================

    @GetMapping("/artisan/events/discussions")
    public String artisanEventDiscussions(
            HttpSession session,
            Model model) {

        // =================================================
        // CHECK LOGIN
        // =================================================

        Object loggedInUserId =
                session.getAttribute("loggedInUserId");

        if (loggedInUserId == null) {
            return "redirect:/login";
        }

        Long userId;

        try {
            userId =
                    Long.valueOf(
                            loggedInUserId.toString()
                    );
        } catch (Exception e) {
            return "redirect:/login";
        }


        // =================================================
        // FIND LOGGED-IN ARTISAN
        // =================================================

        Artisan artisan =
                artisanRepository
                        .findByUserUserId(userId)
                        .orElse(null);

        if (artisan == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "artisan",
                artisan
        );


        // =================================================
        // FIND OPEN WORKSHOP DISCUSSION
        // =================================================

        DiscussionRoom workshopOpenDiscussion =
                discussionRoomRepository
                        .findFirstByEventTypeIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
                                "WORKSHOP",
                                "OPEN"
                        )
                        .orElse(null);


        // =================================================
        // FIND OPEN EXHIBITION DISCUSSION
        // =================================================

        DiscussionRoom exhibitionOpenDiscussion =
                discussionRoomRepository
                        .findFirstByEventTypeIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
                                "EXHIBITION",
                                "OPEN"
                        )
                        .orElse(null);


        // =================================================
        // FIND OPEN COMPETITION DISCUSSION
        // =================================================

        DiscussionRoom competitionOpenDiscussion =
                discussionRoomRepository
                        .findFirstByEventTypeIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
                                "COMPETITION",
                                "OPEN"
                        )
                        .orElse(null);


        // =================================================
        // SEND TO VIEW
        // =================================================

        model.addAttribute(
                "workshopOpenDiscussion",
                workshopOpenDiscussion
        );

        model.addAttribute(
                "exhibitionOpenDiscussion",
                exhibitionOpenDiscussion
        );

        model.addAttribute(
                "competitionOpenDiscussion",
                competitionOpenDiscussion
        );


        // =================================================
        // RETURN PAGE
        // =================================================

        return "artisan-event-discussions";
    }
}
