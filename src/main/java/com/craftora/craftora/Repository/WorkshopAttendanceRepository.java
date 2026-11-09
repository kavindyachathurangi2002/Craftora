
package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkshopAttendanceRepository
        extends JpaRepository<EventRegistration, Long> {

    // Get all registered participants of a workshop
    List<EventRegistration> findByEventId(Long eventId);

    // Get participants by attendance status
    List<EventRegistration>
    findByEventIdAndAttendanceStatusIgnoreCase(
            Long eventId,
            String attendanceStatus
    );

    // Find one participant's registration for a workshop
    Optional<EventRegistration> findByEventIdAndUserId(
            Long eventId,
            Long userId
    );
}

