package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.EventRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRequestRepository
        extends JpaRepository<EventRequest, Long> {

    // =====================================================
    // COUNT REQUESTS BY EVENT TYPE AND STATUS
    // =====================================================

    long countByEventTypeAndStatusIgnoreCase(
            String eventType,
            String status
    );


    // =====================================================
    // GET REQUESTS BY EVENT TYPE AND STATUS
    // =====================================================

    List<EventRequest> findByEventTypeAndStatusIgnoreCaseOrderByCreatedAtDesc(
            String eventType,
            String status
    );


    // =====================================================
    // GET REQUESTS BY ARTISAN
    // =====================================================

    List<EventRequest> findByArtisanArtisanIdOrderByCreatedAtDesc(
            Long artisanId
    );


    // =====================================================
    // GET REQUESTS BY ARTISAN AND EVENT TYPE
    // =====================================================

    List<EventRequest> findByArtisanArtisanIdAndEventTypeIgnoreCase(
            Long artisanId,
            String eventType
    );
}