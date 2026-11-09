
package com.craftora.craftora.Service;

import com.craftora.craftora.Entity.Event;
import com.craftora.craftora.Entity.EventRegistration;
import com.craftora.craftora.Entity.User;
import com.craftora.craftora.Repository.EventRegistrationRepository;
import com.craftora.craftora.Repository.EventRepository;
import com.craftora.craftora.Repository.UserRepository;

import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkshopCertificateDistributionService {

    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public WorkshopCertificateDistributionService(
            EventRegistrationRepository registrationRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            JavaMailSender mailSender) {

        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    @Transactional
    public int distributeCertificates(Long eventId) throws Exception {

        Event workshop = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Workshop not found."));

        if (!"WORKSHOP".equalsIgnoreCase(workshop.getEventType())
                || !"PUBLISHED".equalsIgnoreCase(workshop.getStatus())) {

            throw new IllegalArgumentException(
                    "Only published workshops can distribute certificates.");
        }

        List<EventRegistration> registrations =
                registrationRepository.findByEventId(eventId);

        int sentCount = 0;

        for (EventRegistration registration : registrations) {

            // Customers only
            if (!"CUSTOMER".equalsIgnoreCase(
                    registration.getRegistrationRole())) {
                continue;
            }

            // Attended customers only
            if (!"ATTENDED".equalsIgnoreCase(
                    registration.getAttendanceStatus())) {
                continue;
            }

            User customer = userRepository.findById(
                    registration.getUserId()).orElse(null);

            if (customer == null
                    || customer.getEmail() == null
                    || customer.getEmail().isBlank()) {
                continue;
            }

            MimeMessage message = mailSender.createMimeMessage();

            // No PDF attachment
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(customer.getEmail());
            helper.setSubject(
                    "Your Craftora Workshop Certificate Is Ready");

            String emailBody =
                    "Dear " + customer.getFullName() + ",\n\n"
                    + "Congratulations on attending the Craftora workshop: "
                    + workshop.getEventTitle() + ".\n\n"
                    + "Your Certificate of Participation is available "
                    + "for download on the Craftora website.\n\n"
                    + "Please log in to your Craftora account and visit "
                    + "your Customer Dashboard. Click "
                    + "\"Get My Certificate\" to download your certificate.\n\n"
                    + "If you experience any difficulties downloading your "
                    + "certificate, please contact the Craftora team by "
                    + "sending an email through our website.\n\n"
                    + "Thank you for participating in Craftora.\n\n"
                    + "Best regards,\n"
                    + "Craftora Team";

            helper.setText(emailBody, false);

            // Send email without a PDF attachment
            mailSender.send(message);

            // Make the certificate available on the dashboard
            registration.setCertificateAvailable(true);
            registrationRepository.save(registration);

            sentCount++;
        }

        return sentCount;
    }
}
