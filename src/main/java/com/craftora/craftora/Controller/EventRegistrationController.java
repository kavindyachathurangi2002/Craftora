package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Event;
import com.craftora.craftora.Entity.EventPayment;
import com.craftora.craftora.Entity.EventRegistration;
import com.craftora.craftora.Entity.User;

import com.craftora.craftora.Repository.EventPaymentRepository;
import com.craftora.craftora.Repository.EventRegistrationRepository;
import com.craftora.craftora.Repository.EventRepository;
import com.craftora.craftora.Repository.UserRepository;

import com.craftora.craftora.Service.EventPayHereService;
import com.craftora.craftora.Service.EventRegistrationEmailService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;


@Controller
public class EventRegistrationController {

    private final EventRepository eventRepository;

    private final EventRegistrationRepository eventRegistrationRepository;

    private final EventPaymentRepository eventPaymentRepository;

    private final UserRepository userRepository;

    private final EventPayHereService eventPayHereService;

    private final EventRegistrationEmailService eventRegistrationEmailService;


    public EventRegistrationController(
            EventRepository eventRepository,
            EventRegistrationRepository eventRegistrationRepository,
            EventPaymentRepository eventPaymentRepository,
            UserRepository userRepository,
            EventPayHereService eventPayHereService,
            EventRegistrationEmailService eventRegistrationEmailService) {

        this.eventRepository = eventRepository;

        this.eventRegistrationRepository =
                eventRegistrationRepository;

        this.eventPaymentRepository =
                eventPaymentRepository;

        this.userRepository =
                userRepository;

        this.eventPayHereService =
                eventPayHereService;

        this.eventRegistrationEmailService =
                eventRegistrationEmailService;
    }


    // =====================================================
    // OPEN EVENT REGISTRATION PAGE
    // =====================================================

    @GetMapping("/events/{id}/register")
    public String showRegistrationPage(
            @PathVariable Long id,
            HttpSession session,
            Model model) {


        // -------------------------------------------------
        // 1. LOGIN CHECK
        // -------------------------------------------------

        Object loggedInUserId =
                session.getAttribute("loggedInUserId");

        if (loggedInUserId == null) {

            return "redirect:/login";
        }


        // -------------------------------------------------
        // 2. SELECTED ROLE CHECK
        // -------------------------------------------------

        Object selectedRoleObject =
                session.getAttribute("selectedRole");

        if (selectedRoleObject == null) {

            return "redirect:/select-role";
        }


        String selectedRole =
                selectedRoleObject
                        .toString()
                        .toUpperCase();


        if (!selectedRole.equals("CUSTOMER")
                && !selectedRole.equals("ARTISAN")) {

            return "redirect:/";
        }


        // -------------------------------------------------
        // 3. GET USER
        // -------------------------------------------------

        Long userId =
                Long.valueOf(
                        loggedInUserId.toString()
                );


        User user =
                userRepository
                        .findById(userId)
                        .orElse(null);


        if (user == null) {

            return "redirect:/login";
        }


        // -------------------------------------------------
        // 4. GET EVENT
        // -------------------------------------------------

        Event event =
                eventRepository
                        .findById(id)
                        .orElse(null);


        if (event == null) {

            model.addAttribute(
                    "message",
                    "Event not found."
            );

            return "event-registration-error";
        }


        // -------------------------------------------------
        // 5. EVENT MUST BE PUBLISHED
        // -------------------------------------------------

        if (event.getStatus() == null
                || !event.getStatus()
                        .equalsIgnoreCase("PUBLISHED")) {

            model.addAttribute(
                    "message",
                    "This event is not available for registration."
            );

            return "event-registration-error";
        }


        // -------------------------------------------------
        // 6. ROLE PERMISSION
        // -------------------------------------------------

        if (selectedRole.equals("CUSTOMER")
                && !Boolean.TRUE.equals(
                        event.getAllowCustomers())) {

            model.addAttribute(
                    "message",
                    "Customers are not allowed to register for this event."
            );

            return "event-registration-error";
        }


        if (selectedRole.equals("ARTISAN")
                && !Boolean.TRUE.equals(
                        event.getAllowArtisans())) {

            model.addAttribute(
                    "message",
                    "Artisans are not allowed to register for this event."
            );

            return "event-registration-error";
        }


        // -------------------------------------------------
        // 7. REGISTRATION DEADLINE
        // -------------------------------------------------

        if (event.getRegistrationDeadline() != null
                && LocalDateTime.now()
                        .isAfter(
                                event.getRegistrationDeadline()
                        )) {

            model.addAttribute(
                    "message",
                    "The registration deadline for this event has passed."
            );

            return "event-registration-error";
        }


        // -------------------------------------------------
        // 8. CAPACITY CHECK
        // -------------------------------------------------

        if (isEventFull(event)) {

            model.addAttribute(
                    "message",
                    "Sorry, this event is already full."
            );

            return "event-registration-error";
        }


        // -------------------------------------------------
        // 9. DUPLICATE REGISTRATION CHECK
        // -------------------------------------------------

        var existingRegistration =
                eventRegistrationRepository
                        .findByEventIdAndUserIdAndRegistrationRoleIgnoreCase(
                                event.getEventId(),
                                userId,
                                selectedRole
                        );


        if (existingRegistration.isPresent()) {

            EventRegistration registration =
                    existingRegistration.get();

            String status =
                    registration.getRegistrationStatus();


            if ("CONFIRMED".equalsIgnoreCase(status)) {

                // Send event to error page
                // for Back to Event button

                model.addAttribute(
                        "event",
                        event
                );

                model.addAttribute(
                        "message",
                        "You are already registered for this event."
                );

                return "event-registration-error";
            }


            if ("PENDING_PAYMENT".equalsIgnoreCase(status)) {

                model.addAttribute(
                        "event",
                        event
                );

                model.addAttribute(
                        "message",
                        "You already have a pending payment for this event."
                );

                return "event-registration-error";
            }
        }


        // -------------------------------------------------
        // 10. SEND DATA TO REGISTRATION PAGE
        // -------------------------------------------------

        model.addAttribute(
                "event",
                event
        );


        model.addAttribute(
                "user",
                user
        );


        model.addAttribute(
                "selectedRole",
                selectedRole
        );


        // IMPORTANT:
        // DO NOT SAVE ANYTHING HERE

        return "event-registration";
    }


    // =====================================================
    // CONFIRM EVENT REGISTRATION
    // =====================================================

    @PostMapping("/events/{id}/register")
    public String confirmRegistration(
            @PathVariable Long id,
            @RequestParam(
                    name = "agree",
                    required = false
            ) String agree,
            HttpSession session,
            Model model) {


        // -------------------------------------------------
        // 1. LOGIN CHECK
        // -------------------------------------------------

        Object loggedInUserId =
                session.getAttribute("loggedInUserId");

        if (loggedInUserId == null) {

            return "redirect:/login";
        }


        // -------------------------------------------------
        // 2. AGREEMENT CHECK
        // -------------------------------------------------

        if (!"true".equalsIgnoreCase(agree)) {

            model.addAttribute(
                    "message",
                    "Please confirm that you agree to register for this event."
            );

            return "event-registration-error";
        }


        // -------------------------------------------------
        // 3. SELECTED ROLE CHECK
        // -------------------------------------------------

        Object selectedRoleObject =
                session.getAttribute("selectedRole");

        if (selectedRoleObject == null) {

            return "redirect:/select-role";
        }


        String selectedRole =
                selectedRoleObject
                        .toString()
                        .toUpperCase();


        if (!selectedRole.equals("CUSTOMER")
                && !selectedRole.equals("ARTISAN")) {

            return "redirect:/";
        }


        // -------------------------------------------------
        // 4. GET USER
        // -------------------------------------------------

        Long userId =
                Long.valueOf(
                        loggedInUserId.toString()
                );


        User user =
                userRepository
                        .findById(userId)
                        .orElse(null);


        if (user == null) {

            return "redirect:/login";
        }


        // -------------------------------------------------
        // 5. GET EVENT
        // -------------------------------------------------

        Event event =
                eventRepository
                        .findById(id)
                        .orElse(null);


        if (event == null) {

            model.addAttribute(
                    "message",
                    "Event not found."
            );

            return "event-registration-error";
        }


        // -------------------------------------------------
        // 6. EVENT MUST BE PUBLISHED
        // -------------------------------------------------

        if (event.getStatus() == null
                || !event.getStatus()
                        .equalsIgnoreCase("PUBLISHED")) {

            model.addAttribute(
                    "message",
                    "This event is not available for registration."
            );

            return "event-registration-error";
        }


        // -------------------------------------------------
        // 7. ROLE PERMISSION
        // -------------------------------------------------

        if (selectedRole.equals("CUSTOMER")
                && !Boolean.TRUE.equals(
                        event.getAllowCustomers())) {

            model.addAttribute(
                    "message",
                    "Customers are not allowed to register for this event."
            );

            return "event-registration-error";
        }


        if (selectedRole.equals("ARTISAN")
                && !Boolean.TRUE.equals(
                        event.getAllowArtisans())) {

            model.addAttribute(
                    "message",
                    "Artisans are not allowed to register for this event."
            );

            return "event-registration-error";
        }


        // -------------------------------------------------
        // 8. DEADLINE CHECK
        // -------------------------------------------------

        if (event.getRegistrationDeadline() != null
                && LocalDateTime.now()
                        .isAfter(
                                event.getRegistrationDeadline()
                        )) {

            model.addAttribute(
                    "message",
                    "The registration deadline for this event has passed."
            );

            return "event-registration-error";
        }


        // -------------------------------------------------
        // 9. CAPACITY CHECK
        // -------------------------------------------------

        if (isEventFull(event)) {

            model.addAttribute(
                    "message",
                    "Sorry, this event is already full."
            );

            return "event-registration-error";
        }


        // -------------------------------------------------
        // 10. DUPLICATE CHECK
        // -------------------------------------------------

        var existingRegistration =
                eventRegistrationRepository
                        .findByEventIdAndUserIdAndRegistrationRoleIgnoreCase(
                                event.getEventId(),
                                userId,
                                selectedRole
                        );


        if (existingRegistration.isPresent()) {

            EventRegistration existing =
                    existingRegistration.get();

            String status =
                    existing.getRegistrationStatus();


            if ("CONFIRMED".equalsIgnoreCase(status)) {

                // Send event to error page
                // for Back to Event button

                model.addAttribute(
                        "event",
                        event
                );

                model.addAttribute(
                        "message",
                        "You are already registered for this event."
                );

                return "event-registration-error";
            }


            if ("PENDING_PAYMENT".equalsIgnoreCase(status)) {

                model.addAttribute(
                        "event",
                        event
                );

                model.addAttribute(
                        "message",
                        "You already have a pending payment for this event."
                );

                return "event-registration-error";
            }
        }


        // -------------------------------------------------
        // 11. CALCULATE REGISTRATION FEE
        // -------------------------------------------------

        BigDecimal registrationFee =
                BigDecimal.ZERO;


        if (selectedRole.equals("CUSTOMER")) {

            if (event.getCustomerRegistrationFee() != null) {

                registrationFee =
                        event.getCustomerRegistrationFee();
            }

        } else {

            if (event.getArtisanRegistrationFee() != null) {

                registrationFee =
                        event.getArtisanRegistrationFee();
            }
        }


        // =================================================
        // 12. FREE REGISTRATION
        // =================================================

        if (registrationFee.compareTo(
                BigDecimal.ZERO) <= 0) {


            EventRegistration registration =
                    new EventRegistration();


            registration.setEventId(
                    event.getEventId()
            );


            registration.setUserId(
                    userId
            );


            registration.setRegistrationRole(
                    selectedRole
            );


            registration.setRegistrationStatus(
                    "CONFIRMED"
            );


            registration.setPaymentStatus(
                    "NOT_REQUIRED"
            );


            registration.setAttendanceStatus(
                    "NOT_MARKED"
            );


            registration.setCertificateAvailable(
                    false
            );


            registration.setRegisteredAt(
                    LocalDateTime.now()
            );


            EventRegistration savedRegistration =
                    eventRegistrationRepository.save(
                            registration
                    );


            // =================================================
            // SEND FREE REGISTRATION CONFIRMATION EMAIL
            // =================================================

            try {

                eventRegistrationEmailService
                        .sendFreeRegistrationConfirmation(
                                user,
                                event,
                                selectedRole,
                                savedRegistration.getRegistrationId()
                        );

            } catch (Exception e) {

                System.out.println(
                        "Free event registration email failed: "
                                + e.getMessage()
                );
            }


            // -------------------------------------------------
            // SEND DATA TO SUCCESS PAGE
            // -------------------------------------------------

            model.addAttribute(
                    "event",
                    event
            );


            model.addAttribute(
                    "registration",
                    savedRegistration
            );


            model.addAttribute(
                    "selectedRole",
                    selectedRole
            );


            return "event-registration-success";
        }


        // =================================================
        // 13. PAID REGISTRATION
        // =================================================

        EventRegistration registration =
                new EventRegistration();


        registration.setEventId(
                event.getEventId()
        );


        registration.setUserId(
                userId
        );


        registration.setRegistrationRole(
                selectedRole
        );


        registration.setRegistrationStatus(
                "PENDING_PAYMENT"
        );


        registration.setPaymentStatus(
                "PENDING"
        );


        registration.setAttendanceStatus(
                "NOT_MARKED"
        );


        registration.setCertificateAvailable(
                false
        );


        registration.setRegisteredAt(
                LocalDateTime.now()
        );


        EventRegistration savedRegistration =
                eventRegistrationRepository.save(
                        registration
                );


        // -------------------------------------------------
        // 14. UNIQUE PAYMENT REFERENCE
        // -------------------------------------------------

        String orderId =
                "EVENT-"
                        + event.getEventId()
                        + "-"
                        + savedRegistration
                                .getRegistrationId()
                        + "-"
                        + UUID.randomUUID()
                                .toString()
                                .substring(0, 8)
                                .toUpperCase();


        // -------------------------------------------------
        // 15. CREATE EVENT PAYMENT
        // -------------------------------------------------

        EventPayment payment =
                new EventPayment();


        payment.setRegistrationId(
                savedRegistration
                        .getRegistrationId()
        );


        payment.setEventId(
                event.getEventId()
        );


        payment.setUserId(
                userId
        );


        payment.setAmount(
                registrationFee
        );


        payment.setPaymentMethod(
                "PAYHERE"
        );


        payment.setTransactionId(
                orderId
        );


        payment.setPaymentStatus(
                "PENDING"
        );


        payment.setCreatedAt(
                LocalDateTime.now()
        );


        EventPayment savedPayment =
                eventPaymentRepository.save(
                        payment
                );


        // -------------------------------------------------
        // 16. PREPARE PAYHERE DATA
        // -------------------------------------------------

        String fullName =
                user.getFullName();


        String firstName =
                fullName;


        String lastName =
                "";


        if (fullName != null
                && fullName.trim().contains(" ")) {

            String[] nameParts =
                    fullName
                            .trim()
                            .split("\\s+", 2);


            firstName =
                    nameParts[0];


            lastName =
                    nameParts[1];
        }


        Map<String, String> checkoutData =
                eventPayHereService.createCheckoutData(
                        orderId,
                        firstName,
                        lastName,
                        user.getEmail(),
                        user.getPhone(),
                        "Craftora Event Registration",
                        "Sri Lanka",
                        event.getEventTitle(),
                        registrationFee
                );


        // -------------------------------------------------
        // 17. SEND DATA TO PAYHERE PAGE
        // -------------------------------------------------

        model.addAttribute(
                "checkoutUrl",
                eventPayHereService.getCheckoutUrl()
        );


        model.addAttribute(
                "checkoutData",
                checkoutData
        );


        model.addAttribute(
                "event",
                event
        );


        model.addAttribute(
                "registration",
                savedRegistration
        );


        model.addAttribute(
                "payment",
                savedPayment
        );


        return "event-payment-checkout";
    }


    // =====================================================
    // CHECK EVENT CAPACITY
    // =====================================================

    private boolean isEventFull(Event event) {

        if (event.getCapacity() == null
                || event.getCapacity() <= 0) {

            return false;
        }


        long confirmedCount =
                eventRegistrationRepository
                        .countByEventIdAndRegistrationStatusIgnoreCase(
                                event.getEventId(),
                                "CONFIRMED"
                        );


        return confirmedCount >= event.getCapacity();
    }

// =====================================================
// CUSTOMER - MY EVENT REGISTRATIONS
// =====================================================

@GetMapping("/customer/my-events")
public String showMyEventRegistrations(
        HttpSession session,
        Model model) {

    Object loggedInUserId =
            session.getAttribute("loggedInUserId");

    if (loggedInUserId == null) {
        return "redirect:/login";
    }

    Object selectedRoleObject =
            session.getAttribute("selectedRole");

    if (selectedRoleObject == null) {
        return "redirect:/select-role";
    }

    String selectedRole =
            selectedRoleObject.toString().toUpperCase();

    if (!selectedRole.equals("CUSTOMER")) {
        return "redirect:/";
    }

    Long userId = Long.valueOf(loggedInUserId.toString());

    // Retrieve this user's registrations.
   
var registrations =
        eventRegistrationRepository
                .findByUserIdOrderByRegisteredAtDesc(userId);


    // Build event and registration pairs for the HTML page.
    var registrationDetails = new java.util.ArrayList<
            java.util.Map<String, Object>>();

    for (EventRegistration registration : registrations) {

        Event event = eventRepository
                .findById(registration.getEventId())
                .orElse(null);

        if (event != null) {
            java.util.Map<String, Object> item =
                    new java.util.HashMap<>();

            item.put("event", event);
            item.put("registration", registration);

            registrationDetails.add(item);
        }
    }

    model.addAttribute("registrationDetails", registrationDetails);

    return "customer-my-events";
}
    
}