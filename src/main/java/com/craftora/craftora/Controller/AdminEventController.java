package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Entity.DiscussionRoom;
import com.craftora.craftora.Entity.Event;
import com.craftora.craftora.Entity.User;

import com.craftora.craftora.Repository.ArtisanRepository;
import com.craftora.craftora.Repository.DiscussionRoomRepository;
import com.craftora.craftora.Repository.EventRepository;
import com.craftora.craftora.Repository.UserRepository;

import com.craftora.craftora.Service.EventRequestService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
public class AdminEventController {

    // =====================================================
    // FIELDS
    // =====================================================

    private final EventRequestService eventRequestService;
    private final UserRepository userRepository;
    private final ArtisanRepository artisanRepository;
    private final DiscussionRoomRepository discussionRoomRepository;
    private final EventRepository eventRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public AdminEventController(
            EventRequestService eventRequestService,
            UserRepository userRepository,
            ArtisanRepository artisanRepository,
            DiscussionRoomRepository discussionRoomRepository,
            EventRepository eventRepository) {

        this.eventRequestService = eventRequestService;
        this.userRepository = userRepository;
        this.artisanRepository = artisanRepository;
        this.discussionRoomRepository = discussionRoomRepository;
        this.eventRepository = eventRepository;
    }


    // =====================================================
    // ADMIN EVENTS PAGE
    // =====================================================

    @GetMapping("/admin/events")
    public String adminEvents(
            HttpSession session,
            Model model) {

        // -------------------------------------------------
        // GET LOGGED-IN USER ID
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
        // LOAD ADMIN'S ARTISAN PROFILE
        // -------------------------------------------------

        Artisan artisan =
                artisanRepository
                        .findByUserUserId(user.getUserId())
                        .orElse(null);


        // -------------------------------------------------
        // EVENT REQUEST COUNTS
        // -------------------------------------------------

        long workshopRequests =
                eventRequestService
                        .getPendingRequestCount("WORKSHOP");

        long exhibitionRequests =
                eventRequestService
                        .getPendingRequestCount("EXHIBITION");

        long competitionRequests =
                eventRequestService
                        .getPendingRequestCount("COMPETITION");


        // -------------------------------------------------
        // GET CURRENT OPEN DISCUSSIONS
        // -------------------------------------------------

        Optional<DiscussionRoom> workshopOpenDiscussion =
                discussionRoomRepository
                        .findFirstByEventTypeIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
                                "WORKSHOP",
                                "OPEN"
                        );

        Optional<DiscussionRoom> exhibitionOpenDiscussion =
                discussionRoomRepository
                        .findFirstByEventTypeIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
                                "EXHIBITION",
                                "OPEN"
                        );

        Optional<DiscussionRoom> competitionOpenDiscussion =
                discussionRoomRepository
                        .findFirstByEventTypeIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
                                "COMPETITION",
                                "OPEN"
                        );


        // -------------------------------------------------
        // GET PREVIOUS CLOSED DISCUSSIONS
        // NEWEST → OLDEST
        // -------------------------------------------------

        List<DiscussionRoom> workshopPreviousDiscussions =
                discussionRoomRepository
                        .findByEventTypeIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
                                "WORKSHOP",
                                "CLOSED"
                        );

        List<DiscussionRoom> exhibitionPreviousDiscussions =
                discussionRoomRepository
                        .findByEventTypeIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
                                "EXHIBITION",
                                "CLOSED"
                        );

        List<DiscussionRoom> competitionPreviousDiscussions =
                discussionRoomRepository
                        .findByEventTypeIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
                                "COMPETITION",
                                "CLOSED"
                        );


        // -------------------------------------------------
        // SEND EVENT REQUEST DATA TO HTML
        // -------------------------------------------------

        model.addAttribute(
                "workshopRequests",
                workshopRequests
        );

        model.addAttribute(
                "exhibitionRequests",
                exhibitionRequests
        );

        model.addAttribute(
                "competitionRequests",
                competitionRequests
        );


        // -------------------------------------------------
        // SEND OPEN DISCUSSION DATA TO HTML
        // -------------------------------------------------

        model.addAttribute(
                "workshopOpenDiscussion",
                workshopOpenDiscussion.orElse(null)
        );

        model.addAttribute(
                "exhibitionOpenDiscussion",
                exhibitionOpenDiscussion.orElse(null)
        );

        model.addAttribute(
                "competitionOpenDiscussion",
                competitionOpenDiscussion.orElse(null)
        );


        // -------------------------------------------------
        // SEND PREVIOUS DISCUSSION DATA TO HTML
        // -------------------------------------------------

        model.addAttribute(
                "workshopPreviousDiscussions",
                workshopPreviousDiscussions
        );

        model.addAttribute(
                "exhibitionPreviousDiscussions",
                exhibitionPreviousDiscussions
        );

        model.addAttribute(
                "competitionPreviousDiscussions",
                competitionPreviousDiscussions
        );


        // -------------------------------------------------
        // USER DATA
        // -------------------------------------------------

        model.addAttribute(
                "user",
                user
        );

        model.addAttribute(
                "artisan",
                artisan
        );


        // -------------------------------------------------
        // OPEN ADMIN EVENTS PAGE
        // -------------------------------------------------

        List<Event> publishedEvents =
        eventRepository.findByStatusIgnoreCaseOrderByCreatedAtDesc("PUBLISHED");

model.addAttribute("publishedEvents", publishedEvents);

        return "admin-events";
        
    }


    // =====================================================
    // CREATE EVENT PAGE
    // =====================================================

    @GetMapping("/admin/events/create")
    public String createEventPage(
            HttpSession session,
            Model model) {

        // -------------------------------------------------
        // GET LOGGED-IN USER ID
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
        // SEND EMPTY EVENT OBJECT
        // -------------------------------------------------

        model.addAttribute(
                "event",
                new Event()
        );

        model.addAttribute(
                "user",
                user
        );


        return "admin-create-event";
    }




    

    // =====================================================
    // SAVE / PUBLISH EVENT
    // =====================================================

    @PostMapping("/admin/events/create")
    public String createEvent(

            @RequestParam("eventType")
            String eventType,

            @RequestParam("eventTitle")
            String eventTitle,

            @RequestParam("description")
            String description,

            @RequestParam("eventDate")
            String eventDate,

            @RequestParam("startTime")
            String startTime,

            @RequestParam("endTime")
            String endTime,

            @RequestParam(
                    value = "registrationDeadline",
                    required = false)
            String registrationDeadline,

            @RequestParam(
                    value = "capacity",
                    required = false)
            String capacity,

            @RequestParam("location")
            String location,

            @RequestParam(
                    value = "customLocation",
                    required = false)
            String customLocation,

            @RequestParam(
                    value = "latitude",
                    required = false)
            String latitude,

            @RequestParam(
                    value = "longitude",
                    required = false)
            String longitude,

            @RequestParam(
                    value = "allowCustomers",
                    required = false)
            String allowCustomers,

            @RequestParam(
                    value = "allowArtisans",
                    required = false)
            String allowArtisans,

            @RequestParam(
                    value = "customerRegistrationFee",
                    required = false)
            String customerRegistrationFee,

            @RequestParam(
                    value = "artisanRegistrationFee",
                    required = false)
            String artisanRegistrationFee,

            @RequestParam(
                    value = "rules",
                    required = false)
            String rules,

            @RequestParam(
                    value = "eventImageFile",
                    required = false)
            MultipartFile eventImageFile,

            HttpSession session,

            RedirectAttributes redirectAttributes) {


        // =================================================
        // GET LOGGED-IN USER ID
        // =================================================

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


        // =================================================
        // FIND USER
        // =================================================

        User user =
                userRepository
                        .findById(userId)
                        .orElse(null);

        if (user == null) {
            return "redirect:/login";
        }


        // =================================================
        // CHECK ADMIN ROLE
        // =================================================

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


        // =================================================
        // VALIDATE EVENT TYPE
        // =================================================

        String normalizedEventType =
                eventType == null
                        ? ""
                        : eventType.trim().toUpperCase();

        if (!normalizedEventType.equals("WORKSHOP")
                && !normalizedEventType.equals("EXHIBITION")
                && !normalizedEventType.equals("COMPETITION")) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please select a valid event type."
            );

            return "redirect:/admin/events/create";
        }


        // =================================================
        // VALIDATE EVENT TITLE
        // =================================================

        if (eventTitle == null
                || eventTitle.trim().isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please enter an event title."
            );

            return "redirect:/admin/events/create";
        }


        // =================================================
        // VALIDATE DESCRIPTION
        // =================================================

        if (description == null
                || description.trim().isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please enter an event description."
            );

            return "redirect:/admin/events/create";
        }


        // =================================================
        // CREATE EVENT OBJECT
        // =================================================

        Event event = new Event();

        event.setEventType(
                normalizedEventType
        );

        event.setEventTitle(
                eventTitle.trim()
        );

        event.setDescription(
                description.trim()
        );


        
        // =================================================
        // EVENT DATE VALIDATION
        // Only tomorrow and future dates are allowed
        // =================================================

        try {
            LocalDate selectedEventDate = LocalDate.parse(eventDate);

            
LocalDate minimumEventDate = LocalDate.now().plusDays(1);

if (selectedEventDate.isBefore(minimumEventDate)) {
    redirectAttributes.addFlashAttribute(
        "errorMessage",
        "Event date must be tomorrow or a future date."
    );

    return "redirect:/admin/events/create";
}


            event.setEventDate(selectedEventDate);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Please select a valid event date."
            );

            return "redirect:/admin/events/create";
        }



        // =================================================
        // START TIME
        // =================================================

        try {

            event.setStartTime(
                    LocalTime.parse(startTime)
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please select a valid start time."
            );

            return "redirect:/admin/events/create";
        }


        // =================================================
        // END TIME
        // =================================================

        try {

            event.setEndTime(
                    LocalTime.parse(endTime)
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please select a valid end time."
            );

            return "redirect:/admin/events/create";
        }


        // =================================================
        // CHECK TIME
        // =================================================

        if (!event.getEndTime().isAfter(
                event.getStartTime())) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "End time must be later than start time."
            );

            return "redirect:/admin/events/create";
        }


        // =================================================
        // REGISTRATION DEADLINE
        // =================================================

        if (registrationDeadline != null
                && !registrationDeadline.trim().isEmpty()) {

            try {

                String deadlineValue =
                        registrationDeadline
                                .trim()
                                .replace(" ", "T");

                event.setRegistrationDeadline(
                        LocalDateTime.parse(
                                deadlineValue
                        )
                );

            } catch (Exception e) {

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Please select a valid registration deadline."
                );

                return "redirect:/admin/events/create";
            }
        }


        // =================================================
        // CAPACITY
        // =================================================

        if (capacity != null
                && !capacity.trim().isEmpty()) {

            try {

                int capacityValue =
                        Integer.parseInt(
                                capacity.trim()
                        );

                if (capacityValue <= 0) {

                    redirectAttributes.addFlashAttribute(
                            "errorMessage",
                            "Capacity must be greater than 0."
                    );

                    return "redirect:/admin/events/create";
                }

                event.setCapacity(
                        capacityValue
                );

            } catch (NumberFormatException e) {

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Please enter a valid capacity."
                );

                return "redirect:/admin/events/create";
            }
        }


        // =================================================
        // LOCATION
        // =================================================

        String finalLocation;

        if (location != null
                && location.trim().equalsIgnoreCase("OTHER")) {

            if (customLocation == null
                    || customLocation.trim().isEmpty()) {

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Please enter the custom location name."
                );

                return "redirect:/admin/events/create";
            }

            finalLocation =
                    customLocation.trim();

        } else {

            if (location == null
                    || location.trim().isEmpty()) {

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Please select an event location."
                );

                return "redirect:/admin/events/create";
            }

            finalLocation =
                    location.trim();
        }

        event.setLocation(finalLocation);


        // =================================================
        // LATITUDE & LONGITUDE
        // =================================================

        if (latitude == null
                || latitude.trim().isEmpty()
                || longitude == null
                || longitude.trim().isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please select a valid location on the map."
            );

            return "redirect:/admin/events/create";
        }


        try {

            BigDecimal latitudeValue =
                    new BigDecimal(
                            latitude.trim()
                    );

            BigDecimal longitudeValue =
                    new BigDecimal(
                            longitude.trim()
                    );


            // ---------------------------------------------
            // VALIDATE LATITUDE
            // ---------------------------------------------

            if (latitudeValue.compareTo(
                    new BigDecimal("-90")) < 0
                    ||
                    latitudeValue.compareTo(
                            new BigDecimal("90")) > 0) {

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Latitude must be between -90 and 90."
                );

                return "redirect:/admin/events/create";
            }


            // ---------------------------------------------
            // VALIDATE LONGITUDE
            // ---------------------------------------------

            if (longitudeValue.compareTo(
                    new BigDecimal("-180")) < 0
                    ||
                    longitudeValue.compareTo(
                            new BigDecimal("180")) > 0) {

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Longitude must be between -180 and 180."
                );

                return "redirect:/admin/events/create";
            }


            event.setLatitude(latitudeValue);
            event.setLongitude(longitudeValue);

        } catch (NumberFormatException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please enter valid latitude and longitude values."
            );

            return "redirect:/admin/events/create";
        }


        // =================================================
        // ALLOW CUSTOMERS
        // =================================================

        event.setAllowCustomers(
                allowCustomers != null
        );


        // =================================================
        // ALLOW ARTISANS
        // =================================================

        event.setAllowArtisans(
                allowArtisans != null
        );


        // =================================================
        // CUSTOMER REGISTRATION FEE
        // =================================================

        if (customerRegistrationFee != null
                && !customerRegistrationFee.trim().isEmpty()) {

            try {

                BigDecimal customerFee =
                        new BigDecimal(
                                customerRegistrationFee.trim()
                        );

                if (customerFee.compareTo(
                        BigDecimal.ZERO) < 0) {

                    redirectAttributes.addFlashAttribute(
                            "errorMessage",
                            "Customer registration fee cannot be negative."
                    );

                    return "redirect:/admin/events/create";
                }

                event.setCustomerRegistrationFee(
                        customerFee
                );

            } catch (NumberFormatException e) {

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Please enter a valid customer registration fee."
                );

                return "redirect:/admin/events/create";
            }

        } else {

            event.setCustomerRegistrationFee(
                    BigDecimal.ZERO
            );
        }


        // =================================================
        // ARTISAN REGISTRATION FEE
        // =================================================

        if (artisanRegistrationFee != null
                && !artisanRegistrationFee.trim().isEmpty()) {

            try {

                BigDecimal artisanFee =
                        new BigDecimal(
                                artisanRegistrationFee.trim()
                        );

                if (artisanFee.compareTo(
                        BigDecimal.ZERO) < 0) {

                    redirectAttributes.addFlashAttribute(
                            "errorMessage",
                            "Artisan registration fee cannot be negative."
                    );

                    return "redirect:/admin/events/create";
                }

                event.setArtisanRegistrationFee(
                        artisanFee
                );

            } catch (NumberFormatException e) {

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Please enter a valid artisan registration fee."
                );

                return "redirect:/admin/events/create";
            }

        } else {

            event.setArtisanRegistrationFee(
                    BigDecimal.ZERO
            );
        }


        // =================================================
        // RULES
        // =================================================

        event.setRules(
                rules == null
                        ? null
                        : rules.trim()
        );


        // =================================================
        // EVENT IMAGE UPLOAD
        // =================================================

        if (eventImageFile != null
                && !eventImageFile.isEmpty()) {

            try {

                // -----------------------------------------
                // CHECK IMAGE TYPE
                // -----------------------------------------

                String contentType =
                        eventImageFile.getContentType();

                if (contentType == null
                        || !contentType.startsWith("image/")) {

                    redirectAttributes.addFlashAttribute(
                            "errorMessage",
                            "Please upload a valid image file."
                    );

                    return "redirect:/admin/events/create";
                }


                // -----------------------------------------
                // CREATE UPLOAD DIRECTORY
                // -----------------------------------------

                Path uploadDirectory =
                        Paths.get(
                                "uploads",
                                "events"
                        );

                Files.createDirectories(
                        uploadDirectory
                );


                // -----------------------------------------
                // GET ORIGINAL FILE EXTENSION
                // -----------------------------------------

                String originalFilename =
                        eventImageFile.getOriginalFilename();

                String extension = "";

                if (originalFilename != null
                        && originalFilename.contains(".")) {

                    extension =
                            originalFilename.substring(
                                    originalFilename.lastIndexOf(".")
                            ).toLowerCase();
                }


                // -----------------------------------------
                // GENERATE UNIQUE FILE NAME
                // -----------------------------------------

                String fileName =
                        UUID.randomUUID()
                                + extension;


                // -----------------------------------------
                // SAVE FILE
                // -----------------------------------------

                Path filePath =
                        uploadDirectory.resolve(
                                fileName
                        );

                Files.copy(
                        eventImageFile.getInputStream(),
                        filePath
                );


                // -----------------------------------------
                // SAVE WEB PATH IN DATABASE
                // -----------------------------------------

                event.setEventImage(
                        "/uploads/events/" + fileName
                );

            } catch (IOException e) {

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Unable to upload the event image."
                );

                return "redirect:/admin/events/create";
            }

        } else {

            event.setEventImage(null);
        }


        // =================================================
        // STATUS
        // =================================================

        event.setStatus(
                "PUBLISHED"
        );


        // =================================================
        // CREATED / UPDATED
        // =================================================

        LocalDateTime now =
                LocalDateTime.now();

        event.setCreatedAt(now);
        event.setUpdatedAt(now);


        // =================================================
        // SAVE EVENT
        // =================================================

        eventRepository.save(event);


        // =================================================
        // SUCCESS MESSAGE
        // =================================================

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Event published successfully."
        );


        // =================================================
        // REDIRECT
        // =================================================

        return "redirect:/admin/events";
    }
}