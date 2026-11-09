package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Entity.EventRequest;
import com.craftora.craftora.Entity.User;
import com.craftora.craftora.Repository.ArtisanRepository;
import com.craftora.craftora.Repository.EventRequestRepository;
import com.craftora.craftora.Repository.UserRepository;
import com.craftora.craftora.Service.EventRequestService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class AdminEventRequestController {

    private final EventRequestRepository eventRequestRepository;
    private final EventRequestService eventRequestService;
    private final UserRepository userRepository;
    private final ArtisanRepository artisanRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public AdminEventRequestController(
            EventRequestRepository eventRequestRepository,
            EventRequestService eventRequestService,
            UserRepository userRepository,
            ArtisanRepository artisanRepository) {

        this.eventRequestRepository = eventRequestRepository;
        this.eventRequestService = eventRequestService;
        this.userRepository = userRepository;
        this.artisanRepository = artisanRepository;
    }


    // =====================================================
    // VIEW EVENT REQUESTS
    // =====================================================

    @GetMapping("/admin/events/requests/{eventType}")
    public String viewEventRequests(
            @PathVariable("eventType") String eventType,
            HttpSession session,
            Model model) {


        // -------------------------------------------------
        // GET LOGGED-IN USER
        // -------------------------------------------------

        Object userIdObject =
                session.getAttribute("loggedInUserId");


        if (userIdObject == null) {
            return "redirect:/login";
        }


        Long userId;

        try {

            userId =
                    Long.valueOf(
                            userIdObject.toString()
                    );

        } catch (Exception e) {

            return "redirect:/login";
        }


        // -------------------------------------------------
        // FIND USER
        // -------------------------------------------------

        User user =
                userRepository
                        .findById(userId)
                        .orElse(null);


        if (user == null) {
            return "redirect:/login";
        }


        // -------------------------------------------------
        // CHECK ADMIN ROLE
        // -------------------------------------------------

        boolean isAdmin =
                user.getRoles() != null
                        &&
                user.getRoles()
                        .stream()
                        .anyMatch(
                                role ->
                                        role.getRoleName()
                                                .equalsIgnoreCase("ADMIN")
                        );


        if (!isAdmin) {
            return "redirect:/login";
        }


        // -------------------------------------------------
        // VALIDATE EVENT TYPE
        // -------------------------------------------------

        String normalizedEventType =
                eventType.trim().toUpperCase();


        if (!normalizedEventType.equals("WORKSHOP")
                &&
            !normalizedEventType.equals("EXHIBITION")
                &&
            !normalizedEventType.equals("COMPETITION")) {

            return "redirect:/admin/events";
        }


        // -------------------------------------------------
        // GET PENDING REQUESTS
        // -------------------------------------------------

        List<EventRequest> requests =
                eventRequestRepository
                        .findByEventTypeAndStatusIgnoreCaseOrderByCreatedAtDesc(
                                normalizedEventType,
                                "PENDING"
                        );


        // -------------------------------------------------
        // LOAD ADMIN ARTISAN PROFILE
        // -------------------------------------------------

        Artisan artisan =
                artisanRepository
                        .findByUserUserId(user.getUserId())
                        .orElse(null);


        // -------------------------------------------------
        // PAGE TITLE
        // -------------------------------------------------

        String pageTitle;

        if (normalizedEventType.equals("WORKSHOP")) {

            pageTitle = "Workshop Event Requests";

        } else if (normalizedEventType.equals("EXHIBITION")) {

            pageTitle = "Exhibition Event Requests";

        } else {

            pageTitle = "Competition Event Requests";
        }


        // -------------------------------------------------
        // SEND DATA TO HTML
        // -------------------------------------------------

        model.addAttribute(
                "requests",
                requests
        );


        model.addAttribute(
                "eventType",
                normalizedEventType
        );


        model.addAttribute(
                "pageTitle",
                pageTitle
        );


        model.addAttribute(
                "user",
                user
        );


        model.addAttribute(
                "artisan",
                artisan
        );


        return "admin-event-requests";
    }


    // =====================================================
    // DISMISS EVENT REQUEST
    // =====================================================

    @PostMapping("/admin/events/request/{id}/dismiss")
    public String dismissEventRequest(
            @PathVariable("id") Long id,
            @RequestParam("eventType") String eventType,
            HttpSession session,
            RedirectAttributes redirectAttributes) {


        // -------------------------------------------------
        // GET LOGGED-IN USER
        // -------------------------------------------------

        Object userIdObject =
                session.getAttribute("loggedInUserId");


        if (userIdObject == null) {
            return "redirect:/login";
        }


        Long userId;

        try {

            userId =
                    Long.valueOf(
                            userIdObject.toString()
                    );

        } catch (Exception e) {

            return "redirect:/login";
        }


        // -------------------------------------------------
        // FIND USER
        // -------------------------------------------------

        User user =
                userRepository
                        .findById(userId)
                        .orElse(null);


        if (user == null) {
            return "redirect:/login";
        }


        // -------------------------------------------------
        // CHECK ADMIN
        // -------------------------------------------------

        boolean isAdmin =
                user.getRoles() != null
                        &&
                user.getRoles()
                        .stream()
                        .anyMatch(
                                role ->
                                        role.getRoleName()
                                                .equalsIgnoreCase("ADMIN")
                        );


        if (!isAdmin) {
            return "redirect:/login";
        }


        // -------------------------------------------------
        // FIND REQUEST
        // -------------------------------------------------

        EventRequest request =
                eventRequestRepository
                        .findById(id)
                        .orElse(null);


        if (request == null) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Event request could not be found."
            );

            return "redirect:/admin/events";
        }


        // -------------------------------------------------
        // ONLY PENDING REQUESTS CAN BE DISMISSED
        // -------------------------------------------------

        if (!"PENDING".equalsIgnoreCase(
                request.getStatus())) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This event request has already been reviewed."
            );

            return "redirect:/admin/events/requests/"
                    + eventType;
        }


        // -------------------------------------------------
        // UPDATE STATUS
        // -------------------------------------------------

        eventRequestService.updateStatus(
                id,
                "DISMISSED"
        );


        // -------------------------------------------------
        // SUCCESS MESSAGE
        // -------------------------------------------------

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Event request dismissed successfully."
        );


        return "redirect:/admin/events/requests/"
                + eventType;
    }

    // =========================================================
// CLEAR ALL EVENT REQUESTS
// =========================================================

@PostMapping("/admin/events/requests/clear-all")
public String clearAllEventRequests(
        HttpSession session,
        RedirectAttributes redirectAttributes) {

    Object userIdObject =
            session.getAttribute("loggedInUserId");

    if (userIdObject == null) {
        return "redirect:/login";
    }

    Long userId;

    try {
        userId = Long.valueOf(userIdObject.toString());
    } catch (Exception e) {
        return "redirect:/login";
    }

    User user =
            userRepository.findById(userId)
                    .orElse(null);

    if (user == null) {
        return "redirect:/login";
    }

    // ADMIN CHECK
    boolean isAdmin =
            user.getRoles() != null
            && user.getRoles()
                    .stream()
                    .anyMatch(role ->
                            role.getRoleName()
                                    .equalsIgnoreCase("ADMIN"));

    if (!isAdmin) {
        return "redirect:/login";
    }

    // DELETE ALL EVENT REQUESTS
    eventRequestRepository.deleteAllInBatch();

    redirectAttributes.addFlashAttribute(
            "successMessage",
            "All event requests have been cleared successfully."
    );

    return "redirect:/admin/events";
}
}