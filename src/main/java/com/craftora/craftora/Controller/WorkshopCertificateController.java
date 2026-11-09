
package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Event;
import com.craftora.craftora.Entity.EventRegistration;
import com.craftora.craftora.Entity.User;
import com.craftora.craftora.Repository.EventRegistrationRepository;
import com.craftora.craftora.Repository.EventRepository;
import com.craftora.craftora.Repository.UserRepository;
import com.craftora.craftora.Service.WorkshopCertificateService;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class WorkshopCertificateController {

    private final WorkshopCertificateService certificateService;
    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public WorkshopCertificateController(
            WorkshopCertificateService certificateService,
            EventRegistrationRepository registrationRepository,
            EventRepository eventRepository,
            UserRepository userRepository) {

        this.certificateService = certificateService;
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    // =====================================================
    // GET LOGGED-IN CUSTOMER ID
    // =====================================================

    private Long getLoggedInCustomerId(HttpSession session) {

        Object userIdObject =
                session.getAttribute("loggedInUserId");

        if (userIdObject == null) {
            return null;
        }

        try {
            Long userId = Long.valueOf(userIdObject.toString());

            User customer = userRepository.findById(userId)
                    .orElse(null);

            if (customer == null) {
                return null;
            }

            return userId;

        } catch (NumberFormatException e) {
            return null;
        }
    }

    // =====================================================
    // CHECK CERTIFICATE ACCESS
    // =====================================================

    private boolean isCertificateAvailable(
            EventRegistration registration,
            Long customerId) {

        if (registration == null || customerId == null) {
            return false;
        }

        if (registration.getUserId() == null
                || !customerId.equals(registration.getUserId())) {
            return false;
        }

        if (!"CUSTOMER".equalsIgnoreCase(
                registration.getRegistrationRole())) {
            return false;
        }

        if (!"ATTENDED".equalsIgnoreCase(
                registration.getAttendanceStatus())) {
            return false;
        }

        boolean certificateDistributed =
                Boolean.TRUE.equals(
                        registration.getCertificateAvailable());

        boolean certificatePreviouslyCreated =
                registration.getCertificateId() != null
                && !registration.getCertificateId().isBlank();

        return certificateDistributed || certificatePreviouslyCreated;
    }

    // =====================================================
    // CERTIFICATE PREVIEW PAGE
    // =====================================================

    @GetMapping("/customer/workshop-certificate/{registrationId}")
    public String viewCertificate(
            @PathVariable Long registrationId,
            HttpSession session,
            Model model) {

        Long customerId = getLoggedInCustomerId(session);

        if (customerId == null) {
            return "redirect:/login";
        }

        EventRegistration registration =
                registrationRepository.findById(registrationId)
                        .orElse(null);

        if (!isCertificateAvailable(registration, customerId)) {
            return "redirect:/customer-dashboard";
        }

        Event workshop = eventRepository.findById(
                registration.getEventId())
                .orElse(null);

        if (workshop == null
                || !"WORKSHOP".equalsIgnoreCase(
                        workshop.getEventType())) {
            return "redirect:/customer-dashboard";
        }

        User customer = userRepository.findById(customerId)
                .orElse(null);

        if (customer == null) {
            return "redirect:/login";
        }

        model.addAttribute("registration", registration);
        model.addAttribute("workshop", workshop);
        model.addAttribute("customer", customer);

        return "workshop-certificate-preview";
    }

    // =====================================================
    // DOWNLOAD CERTIFICATE PDF
    // =====================================================

    @GetMapping("/customer/workshop-certificate/{registrationId}/download")
    public ResponseEntity<byte[]> downloadCertificate(
            @PathVariable Long registrationId,
            HttpSession session) throws Exception {

        Long customerId = getLoggedInCustomerId(session);

        if (customerId == null) {
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, "/login")
                    .build();
        }

        EventRegistration registration =
                registrationRepository.findById(registrationId)
                        .orElse(null);

        if (!isCertificateAvailable(registration, customerId)) {
            return ResponseEntity.notFound().build();
        }

        Event workshop = eventRepository.findById(
                registration.getEventId())
                .orElse(null);

        if (workshop == null
                || !"WORKSHOP".equalsIgnoreCase(
                        workshop.getEventType())) {
            return ResponseEntity.notFound().build();
        }

        // Generate the PDF first.
        byte[] pdf = certificateService.generateCertificate(
                registrationId, customerId);

        // Reload to get the certificate ID generated by the service.
        EventRegistration updatedRegistration =
                registrationRepository.findById(registrationId)
                        .orElse(registration);

        String certificateId =
                updatedRegistration.getCertificateId();

        if (certificateId == null || certificateId.isBlank()) {
            certificateId = "Craftora-Certificate";
        }

        // Remove the dashboard notification.
        // Keep the certificate ID so the customer can download again.
        updatedRegistration.setCertificateAvailable(false);
        registrationRepository.save(updatedRegistration);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + certificateId + ".pdf\"")
                .body(pdf);
    }
}
