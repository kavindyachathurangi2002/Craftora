
package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Event;
import com.craftora.craftora.Entity.EventRegistration;
import com.craftora.craftora.Repository.EventRegistrationRepository;
import com.craftora.craftora.Repository.EventRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ArtisanEventRegistrationsController {

    private final EventRegistrationRepository eventRegistrationRepository;
    private final EventRepository eventRepository;

    public ArtisanEventRegistrationsController(
            EventRegistrationRepository eventRegistrationRepository,
            EventRepository eventRepository) {

        this.eventRegistrationRepository = eventRegistrationRepository;
        this.eventRepository = eventRepository;
    }

    @GetMapping("/artisan/events/registrations")
    public String myEventRegistrations(
            HttpSession session,
            Model model) {

        Object loggedInUserId =
                session.getAttribute("loggedInUserId");

        if (loggedInUserId == null) {
            return "redirect:/login";
        }

        Object selectedRole =
                session.getAttribute("selectedRole");

        if (selectedRole == null
                || !"ARTISAN".equalsIgnoreCase(
                        selectedRole.toString())) {
            return "redirect:/select-role";
        }

        Long userId =
                Long.valueOf(loggedInUserId.toString());

        List<EventRegistration> registrations =
                eventRegistrationRepository
                        .findByUserIdOrderByRegisteredAtDesc(userId);

        List<Map<String, Object>> registrationDetails =
                new ArrayList<>();

        for (EventRegistration registration : registrations) {

            // Show only the artisan's registrations
            if (!"ARTISAN".equalsIgnoreCase(
                    registration.getRegistrationRole())) {
                continue;
            }

            Event event = eventRepository
                    .findById(registration.getEventId())
                    .orElse(null);

            if (event == null) {
                continue;
            }

            Map<String, Object> item = new HashMap<>();

            item.put("registration", registration);
            item.put("event", event);

            registrationDetails.add(item);
        }

        model.addAttribute(
                "registrationDetails",
                registrationDetails
        );

        return "artisan-event-registrations";
    }
}
