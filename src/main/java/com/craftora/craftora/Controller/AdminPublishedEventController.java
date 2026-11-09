
package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Event;
import com.craftora.craftora.Repository.EventRepository;
import com.craftora.craftora.Entity.User;
import com.craftora.craftora.Repository.UserRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;



import java.util.List;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Controller
public class AdminPublishedEventController {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/admin/events/published")
    public String publishedEventsPage(
            HttpSession session,
            Model model) {

        Long userId =
                (Long) session.getAttribute("loggedInUserId");

        if (userId == null) {
            return "redirect:/login";
        }

        User user = userRepository.findById(userId)
                .orElse(null);

        if (user == null) {
    return "redirect:/login";
}

boolean isAdmin =
        user.getRoles() != null
                && user.getRoles()
                .stream()
                .anyMatch(role ->
                        role.getRoleName()
                                .equalsIgnoreCase("ADMIN")
                );

if (!isAdmin) {
    return "redirect:/login";
}

        List<Event> publishedEvents =
                eventRepository
                .findByStatusIgnoreCaseOrderByCreatedAtDesc(
                        "PUBLISHED");

        model.addAttribute("publishedEvents", publishedEvents);
        model.addAttribute("user", user);
       
    
        return "admin-published-events";
    }

    
@PostMapping("/admin/events/delete/{id}")
public String deletePublishedEvent(
        @PathVariable("id") Long id,
        HttpSession session,
        RedirectAttributes redirectAttributes) {

    Long userId =
            (Long) session.getAttribute("loggedInUserId");

    if (userId == null) {
        return "redirect:/login";
    }

    User user = userRepository.findById(userId)
            .orElse(null);

    if (user == null) {
        return "redirect:/login";
    }

    boolean isAdmin =
            user.getRoles() != null
                    && user.getRoles()
                    .stream()
                    .anyMatch(role ->
                            role.getRoleName()
                                    .equalsIgnoreCase("ADMIN")
                    );

    if (!isAdmin) {
        return "redirect:/login";
    }

    Event event = eventRepository.findById(id)
            .orElse(null);

    if (event == null
            || !"PUBLISHED".equalsIgnoreCase(event.getStatus())) {

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Published event not found."
        );

        return "redirect:/admin/events/published";
    }

    eventRepository.delete(event);

    redirectAttributes.addFlashAttribute(
            "successMessage",
            "Event deleted successfully."
    );

    return "redirect:/admin/events/published";
}
@GetMapping("/admin/events/edit/{id}")
public String editPublishedEvent(
        @PathVariable("id") Long id,
        HttpSession session,
        Model model) {

    Long userId =
            (Long) session.getAttribute("loggedInUserId");

    if (userId == null) {
        return "redirect:/login";
    }

    User user = userRepository.findById(userId)
            .orElse(null);

    if (user == null) {
        return "redirect:/login";
    }

    boolean isAdmin =
            user.getRoles() != null
                    && user.getRoles()
                    .stream()
                    .anyMatch(role ->
                            role.getRoleName()
                                    .equalsIgnoreCase("ADMIN")
                    );

    if (!isAdmin) {
        return "redirect:/login";
    }

    Event event = eventRepository.findById(id)
            .orElse(null);

    if (event == null
            || !"PUBLISHED".equalsIgnoreCase(event.getStatus())) {
        return "redirect:/admin/events/published";
    }

    model.addAttribute("event", event);
    model.addAttribute("user", user);

    return "admin-edit-event";
}



@PostMapping("/admin/events/edit/{id}")
public String updatePublishedEvent(
        @PathVariable("id") Long id,
        @RequestParam("eventType") String eventType,
        @RequestParam("eventTitle") String eventTitle,
        @RequestParam("description") String description,
        @RequestParam("eventDate") String eventDate,
        @RequestParam("startTime") String startTime,
        @RequestParam("endTime") String endTime,
        @RequestParam("registrationDeadline") String registrationDeadline,
        @RequestParam(value = "capacity", required = false) String capacity,
        @RequestParam("location") String location,
        @RequestParam(value = "latitude", required = false) String latitude,
        @RequestParam(value = "longitude", required = false) String longitude,
        @RequestParam(value = "allowCustomers", required = false) String allowCustomers,
        @RequestParam(value = "allowArtisans", required = false) String allowArtisans,
        @RequestParam(value = "customerRegistrationFee", required = false) String customerFee,
        @RequestParam(value = "artisanRegistrationFee", required = false) String artisanFee,
        @RequestParam(value = "rules", required = false) String rules,
        @RequestParam(value = "eventImageFile", required = false) MultipartFile eventImageFile,
        HttpSession session,
        RedirectAttributes redirectAttributes) {

    Long userId = (Long) session.getAttribute("loggedInUserId");

    if (userId == null) {
        return "redirect:/login";
    }

    User user = userRepository.findById(userId).orElse(null);

    if (user == null || user.getRoles() == null
            || user.getRoles().stream()
            .noneMatch(role -> role.getRoleName().equalsIgnoreCase("ADMIN"))) {
        return "redirect:/login";
    }

    Event event = eventRepository.findById(id).orElse(null);

    if (event == null || !"PUBLISHED".equalsIgnoreCase(event.getStatus())) {
        redirectAttributes.addFlashAttribute(
                "errorMessage", "Published event not found.");
        return "redirect:/admin/events/published";
    }

    try {
        if (eventType == null ||
                !(eventType.equals("WORKSHOP")
                || eventType.equals("EXHIBITION")
                || eventType.equals("COMPETITION"))) {
            throw new IllegalArgumentException("Please select a valid event type.");
        }

        if (eventTitle == null || eventTitle.trim().isEmpty()
                || description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Event title and description are required.");
        }

        LocalDate parsedDate = LocalDate.parse(eventDate);
        LocalTime parsedStart = LocalTime.parse(startTime);
        LocalTime parsedEnd = LocalTime.parse(endTime);
        LocalDateTime parsedDeadline = LocalDateTime.parse(registrationDeadline);

        if (!parsedEnd.isAfter(parsedStart)) {
            throw new IllegalArgumentException(
                    "End time must be later than start time.");
        }

        if (location == null || location.trim().isEmpty()) {
            throw new IllegalArgumentException("Event location is required.");
        }

        if (latitude == null || latitude.trim().isEmpty()
                || longitude == null || longitude.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Latitude and longitude are required.");
        }

        BigDecimal lat = new BigDecimal(latitude.trim());
        BigDecimal lng = new BigDecimal(longitude.trim());

        if (lat.compareTo(new BigDecimal("-90")) < 0
                || lat.compareTo(new BigDecimal("90")) > 0
                || lng.compareTo(new BigDecimal("-180")) < 0
                || lng.compareTo(new BigDecimal("180")) > 0) {
            throw new IllegalArgumentException(
                    "Please enter valid location coordinates.");
        }

        Integer parsedCapacity = null;

        if (capacity != null && !capacity.trim().isEmpty()) {
            parsedCapacity = Integer.parseInt(capacity.trim());

            if (parsedCapacity <= 0) {
                throw new IllegalArgumentException(
                        "Capacity must be greater than zero.");
            }
        }

        BigDecimal parsedCustomerFee =
                customerFee == null || customerFee.trim().isEmpty()
                        ? BigDecimal.ZERO : new BigDecimal(customerFee.trim());

        BigDecimal parsedArtisanFee =
                artisanFee == null || artisanFee.trim().isEmpty()
                        ? BigDecimal.ZERO : new BigDecimal(artisanFee.trim());

        if (parsedCustomerFee.compareTo(BigDecimal.ZERO) < 0
                || parsedArtisanFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Registration fees cannot be negative.");
        }

        // Update the existing event; do not create a new event.
        event.setEventType(eventType);
        event.setEventTitle(eventTitle.trim());
        event.setDescription(description.trim());
        event.setEventDate(parsedDate);
        event.setStartTime(parsedStart);
        event.setEndTime(parsedEnd);
        event.setRegistrationDeadline(parsedDeadline);
        event.setCapacity(parsedCapacity);
        event.setLocation(location.trim());
        event.setLatitude(lat);
        event.setLongitude(lng);
        event.setAllowCustomers(allowCustomers != null);
        event.setAllowArtisans(allowArtisans != null);
        event.setCustomerRegistrationFee(parsedCustomerFee);
        event.setArtisanRegistrationFee(parsedArtisanFee);
        event.setRules(rules == null ? null : rules.trim());
        event.setUpdatedAt(LocalDateTime.now());

        // Keep the current image unless a new image is uploaded.
        if (eventImageFile != null && !eventImageFile.isEmpty()) {

            String contentType = eventImageFile.getContentType();

            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException(
                        "Please upload a valid image file.");
            }

            Path uploadDirectory = Paths.get("uploads", "events");
            Files.createDirectories(uploadDirectory);

            String originalFilename = eventImageFile.getOriginalFilename();
            String extension = "";

            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(
                        originalFilename.lastIndexOf(".")).toLowerCase();
            }

            String fileName = UUID.randomUUID() + extension;

            Files.copy(
                    eventImageFile.getInputStream(),
                    uploadDirectory.resolve(fileName));

            event.setEventImage("/uploads/events/" + fileName);
        }

        // Preserve PUBLISHED status.
        event.setStatus("PUBLISHED");

        eventRepository.save(event);

        redirectAttributes.addFlashAttribute(
                "successMessage", "Event updated successfully.");

        return "redirect:/admin/events/published";

    } catch (IllegalArgumentException e) {
        redirectAttributes.addFlashAttribute(
                "errorMessage", e.getMessage());

        return "redirect:/admin/events/edit/" + id;

    } catch (IOException e) {
        redirectAttributes.addFlashAttribute(
                "errorMessage", "Unable to upload the event image.");

        return "redirect:/admin/events/edit/" + id;
    }
}

    
}
