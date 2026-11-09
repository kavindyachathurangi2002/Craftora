
package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Event;
import com.craftora.craftora.Entity.EventRegistration;
import com.craftora.craftora.Entity.User;
import com.craftora.craftora.Repository.EventRepository;
import com.craftora.craftora.Repository.UserRepository;
import com.craftora.craftora.Repository.WorkshopAttendanceRepository;
import com.craftora.craftora.Service.WorkshopCertificateDistributionService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class AdminWorkshopAttendanceController {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final WorkshopAttendanceRepository attendanceRepository;
    private final WorkshopCertificateDistributionService certificateDistributionService;

   public AdminWorkshopAttendanceController(
        EventRepository eventRepository,
        UserRepository userRepository,
        WorkshopAttendanceRepository attendanceRepository,
        WorkshopCertificateDistributionService certificateDistributionService) {

    this.eventRepository = eventRepository;
    this.userRepository = userRepository;
    this.attendanceRepository = attendanceRepository;
    this.certificateDistributionService = certificateDistributionService;
}

    // =====================================================
    // CHECK ADMIN ACCESS
    // =====================================================

    private User getLoggedInAdmin(HttpSession session) {

        Object userIdObject =
                session.getAttribute("loggedInUserId");

        if (userIdObject == null) {
            return null;
        }

        Long userId;

        try {
            userId = Long.valueOf(userIdObject.toString());
        } catch (NumberFormatException e) {
            return null;
        }

        User user = userRepository.findById(userId)
                .orElse(null);

        if (user == null || user.getRoles() == null) {
            return null;
        }

        boolean isAdmin = user.getRoles()
                .stream()
                .anyMatch(role ->
                        role.getRoleName() != null
                                && role.getRoleName()
                                .equalsIgnoreCase("ADMIN"));

        return isAdmin ? user : null;
    }

    // =====================================================
    // CHECK PUBLISHED WORKSHOP
    // =====================================================

    private Event getPublishedWorkshop(Long eventId) {

        if (eventId == null) {
            return null;
        }

        Event workshop = eventRepository.findById(eventId)
                .orElse(null);

        if (workshop == null
                || workshop.getEventType() == null
                || !workshop.getEventType()
                .equalsIgnoreCase("WORKSHOP")
                || workshop.getStatus() == null
                || !workshop.getStatus()
                .equalsIgnoreCase("PUBLISHED")) {
            return null;
        }

        return workshop;
    }

    // =====================================================
    // SHOW WORKSHOP SELECTION PAGE
    // =====================================================

    @GetMapping("/admin/workshop-attendance")
    public String showWorkshopAttendance(
            HttpSession session,
            Model model) {

        User user = getLoggedInAdmin(session);

        if (user == null) {
            return "redirect:/login";
        }

        List<Event> workshops =
                eventRepository
                        .findByStatusIgnoreCaseAndEventTypeIgnoreCase(
                                "PUBLISHED",
                                "WORKSHOP");

        model.addAttribute("workshops", workshops);
        model.addAttribute("user", user);

        return "admin-workshop-attendance";
    }

    // =====================================================
    // SHOW WORKSHOP PARTICIPANTS
    // =====================================================

    @GetMapping("/admin/workshop-attendance/participants")
    public String showParticipants(
            @RequestParam("eventId") Long eventId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        User user = getLoggedInAdmin(session);

        if (user == null) {
            return "redirect:/login";
        }

        Event workshop = getPublishedWorkshop(eventId);

        if (workshop == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Published workshop not found.");

            return "redirect:/admin/workshop-attendance";
        }

        List<EventRegistration> registrations =
                attendanceRepository.findByEventId(eventId)
                        .stream()
                        .filter(registration ->
                                "CUSTOMER".equalsIgnoreCase(
                                        registration.getRegistrationRole()))
                        .filter(registration ->
                                "REGISTERED".equalsIgnoreCase(
                                        registration.getRegistrationStatus())
                                || "CONFIRMED".equalsIgnoreCase(
                                        registration.getRegistrationStatus()))
                        .toList();

        Map<Long, User> participantUsers = new HashMap<>();

        for (EventRegistration registration : registrations) {
            userRepository.findById(registration.getUserId())
                    .ifPresent(participant ->
                            participantUsers.put(
                                    registration.getRegistrationId(),
                                    participant));
        }

        // Check whether certificates are available for any
        // attended customer in this workshop.
        boolean certificatesDistributed =
                attendanceRepository.findByEventId(eventId)
                        .stream()
                        .anyMatch(registration ->
                                "CUSTOMER".equalsIgnoreCase(
                                        registration.getRegistrationRole())
                                && "ATTENDED".equalsIgnoreCase(
                                        registration.getAttendanceStatus())
                                && Boolean.TRUE.equals(
                                        registration.getCertificateAvailable()));

        model.addAttribute("user", user);
        model.addAttribute("workshop", workshop);
        model.addAttribute("registrations", registrations);
        model.addAttribute("participantUsers", participantUsers);
        model.addAttribute(
                "certificatesDistributed",
                certificatesDistributed);

        return "admin-workshop-participants";
    }

// =====================================================
// SAVE WORKSHOP ATTENDANCE
// =====================================================

@PostMapping("/admin/workshop-attendance/save")
@Transactional
public String saveAttendance(
        @RequestParam("eventId") Long eventId,
        @RequestParam Map<String, String> formData,
        HttpSession session,
        RedirectAttributes redirectAttributes) {

    User user = getLoggedInAdmin(session);

    if (user == null) {
        return "redirect:/login";
    }

    Event workshop = getPublishedWorkshop(eventId);

    if (workshop == null) {
        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Published workshop not found.");

        return "redirect:/admin/workshop-attendance";
    }

    List<EventRegistration> registrations =
            attendanceRepository.findByEventId(eventId);

    int updatedCount = 0;

    for (EventRegistration registration : registrations) {

        // Customers only
        if (!"CUSTOMER".equalsIgnoreCase(
                registration.getRegistrationRole())) {
            continue;
        }

        // Only registered or confirmed customers
        String registrationStatus =
                registration.getRegistrationStatus();

        if (registrationStatus == null
                || (!"REGISTERED".equalsIgnoreCase(registrationStatus)
                && !"CONFIRMED".equalsIgnoreCase(registrationStatus))) {
            continue;
        }

        String status = formData.get(
                "attendance_" + registration.getRegistrationId());

        if (!"ATTENDED".equals(status)
                && !"NOT_ATTENDED".equals(status)) {
            continue;
        }

        registration.setAttendanceStatus(status);
        registration.setAttendanceMarkedAt(LocalDateTime.now());

        attendanceRepository.save(registration);
        updatedCount++;
    }

    redirectAttributes.addFlashAttribute(
            "successMessage",
            "Attendance saved successfully. Updated customers: "
                    + updatedCount);

    return "redirect:/admin/workshop-attendance/participants?eventId="
            + eventId;
}


// =====================================================
// DISTRIBUTE WORKSHOP CERTIFICATES
// =====================================================

@PostMapping("/admin/workshop-attendance/distribute")
public String distributeCertificates(
        @RequestParam("eventId") Long eventId,
        HttpSession session,
        RedirectAttributes redirectAttributes) {

    User user = getLoggedInAdmin(session);

    if (user == null) {
        return "redirect:/login";
    }

    Event workshop = getPublishedWorkshop(eventId);

    if (workshop == null) {
        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Published workshop not found.");

        return "redirect:/admin/workshop-attendance";
    }

    try {
        int sentCount =
                certificateDistributionService
                        .distributeCertificates(eventId);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Certificate notifications distributed successfully. "
                        + "Emails sent: " + sentCount);

    } catch (Exception e) {

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Unable to distribute certificates. Please check "
                        + "the email configuration and try again.");
    }

    return "redirect:/admin/workshop-attendance/participants?eventId="
            + eventId;
}
    
}
