package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStatusIgnoreCase(String status);

    List<Event> findByEventTypeIgnoreCase(String eventType);

    List<Event> findByArtisanId(Long artisanId);

    List<Event> findByStatusIgnoreCaseAndEventTypeIgnoreCase(
            String status,
            String eventType
    );

    List<Event> findByStatusIgnoreCaseOrderByCreatedAtDesc(
            String status
    );

    // =====================================================
    // ARTISAN UPCOMING EVENTS
    // =====================================================

    List<Event> findByStatusIgnoreCaseAndAllowArtisansTrueAndEventDateGreaterThanEqualOrderByEventDateAsc(
            String status,
            LocalDate eventDate
    );
}