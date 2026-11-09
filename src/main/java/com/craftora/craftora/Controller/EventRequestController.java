package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Repository.ArtisanRepository;
import com.craftora.craftora.Service.EventRequestService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EventRequestController {

    private final EventRequestService eventRequestService;
    private final ArtisanRepository artisanRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public EventRequestController(
            EventRequestService eventRequestService,
            ArtisanRepository artisanRepository) {

        this.eventRequestService = eventRequestService;
        this.artisanRepository = artisanRepository;
    }


    // =====================================================
    // SHOW REQUEST EVENT PAGE
    // =====================================================

    @GetMapping("/artisan/events/request")
    public String showRequestEventPage(
            HttpSession session,
            Model model) {

        Object loggedInUserId =
                session.getAttribute("loggedInUserId");

        // User must be logged in
        if (loggedInUserId == null) {
            return "redirect:/login";
        }

        Long userId =
                Long.valueOf(loggedInUserId.toString());


        // Find artisan using logged-in user
        Artisan artisan =
                artisanRepository.findByUserUserId(userId)
                        .orElse(null);


        // User must have an artisan profile
        if (artisan == null) {
            return "redirect:/artisan-dashboard";
        }


        model.addAttribute("artisan", artisan);

        return "request-event";
    }


    // =====================================================
    // SUBMIT EVENT REQUEST
    // =====================================================

    @PostMapping("/artisan/events/request")
    public String submitEventRequest(
            @RequestParam("eventType") String eventType,
            @RequestParam("requestDescription")
                    String requestDescription,
            HttpSession session,
            RedirectAttributes redirectAttributes) {


        Object loggedInUserId =
                session.getAttribute("loggedInUserId");


        // User must be logged in
        if (loggedInUserId == null) {
            return "redirect:/login";
        }


        Long userId =
                Long.valueOf(loggedInUserId.toString());


        // Find artisan
        Artisan artisan =
                artisanRepository.findByUserUserId(userId)
                        .orElse(null);


        if (artisan == null) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Artisan profile not found."
            );

            return "redirect:/artisan-dashboard";
        }


        // =================================================
        // VALIDATE EVENT TYPE
        // =================================================

        if (eventType == null ||
                (!eventType.equalsIgnoreCase("WORKSHOP")
                && !eventType.equalsIgnoreCase("EXHIBITION")
                && !eventType.equalsIgnoreCase("COMPETITION"))) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please select a valid event type."
            );

            return "redirect:/artisan/events/request";
        }

        // =================================================
// CHECK FOR EXISTING ACTIVE REQUEST
// =================================================

if (!eventRequestService.canSubmitRequest(
        artisan.getArtisanId(),
        eventType)) {

    redirectAttributes.addFlashAttribute(
            "errorMessage",
            "You already have a request for this event type. " +
            "You can submit another request only after the previous request is dismissed."
    );

    return "redirect:/artisan/events/request";
}


        // =================================================
        // VALIDATE DESCRIPTION
        // =================================================

        if (requestDescription == null ||
                requestDescription.trim().isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please tell us about your event idea."
            );

            return "redirect:/artisan/events/request";
        }


        // =================================================
        // SAVE REQUEST
        // =================================================

        eventRequestService.createRequest(
                artisan,
                eventType.toUpperCase(),
                requestDescription.trim()
        );


        // =================================================
        // SUCCESS MESSAGE
        // =================================================

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Your event request has been submitted successfully."
        );


        return "redirect:/artisan/events/request";
    }
}