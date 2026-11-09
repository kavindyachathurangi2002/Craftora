package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRegistrationRepository
        extends JpaRepository<EventRegistration, Long> {

    // All registrations for one event
    List<EventRegistration> findByEventId(Long eventId);

    // All event registrations of one user
    List<EventRegistration> findByUserIdOrderByRegisteredAtDesc(Long userId);

    // Check whether a user already has this registration role
    Optional<EventRegistration>
    findByEventIdAndUserIdAndRegistrationRoleIgnoreCase(
            Long eventId,
            Long userId,
            String registrationRole
    );

    // Count registrations for capacity checking
    long countByEventIdAndRegistrationStatusIgnoreCase(
            Long eventId,
            String registrationStatus
    );

    // Registrations by role
    List<EventRegistration>
    findByEventIdAndRegistrationRoleIgnoreCase(
            Long eventId,
            String registrationRole
    );

    // Workshop attendance list
    List<EventRegistration>
    findByEventIdAndAttendanceStatusIgnoreCase(
            Long eventId,
            String attendanceStatus
    );

    // Payment status
    List<EventRegistration>
    findByEventIdAndPaymentStatusIgnoreCase(
            Long eventId,
            String paymentStatus
    );
}