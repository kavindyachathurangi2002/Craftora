package com.craftora.craftora.Service;

import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Entity.EventRequest;
import com.craftora.craftora.Repository.EventRequestRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventRequestService {

    private final EventRequestRepository eventRequestRepository;

    public EventRequestService(EventRequestRepository eventRequestRepository) {
        this.eventRequestRepository = eventRequestRepository;
    }

    // =========================================================
    // CHECK WHETHER ARTISAN CAN SUBMIT A REQUEST
    // =========================================================
    public boolean canSubmitRequest(Long artisanId, String eventType) {

        String normalizedEventType =
                eventType == null
                        ? ""
                        : eventType.trim().toUpperCase();

        List<EventRequest> existingRequests =
                eventRequestRepository
                        .findByArtisanArtisanIdAndEventTypeIgnoreCase(
                                artisanId,
                                normalizedEventType
                        );

        /*
         * SAME EVENT TYPE:
         *
         * PENDING
         * -> Cannot submit another request
         *
         * DISCUSSION_OPEN
         * -> Cannot submit another request
         *
         * DISCUSSION_CLOSED
         * -> Can submit another request
         *
         * DISMISSED
         * -> Can submit another request
         *
         * DIFFERENT EVENT TYPES are checked separately.
         *
         * Example:
         * Competition = PENDING
         * Workshop    = Can request
         * Exhibition  = Can request
         */

        for (EventRequest request : existingRequests) {

            String status = request.getStatus();

            if (status == null) {
                continue;
            }

            status = status.trim().toUpperCase();

            if (status.equals("PENDING")
                    || status.equals("DISCUSSION_OPEN")) {

                return false;
            }
        }

        return true;
    }


    // =========================================================
    // CREATE NEW EVENT REQUEST
    // =========================================================
    public EventRequest createRequest(
            Artisan artisan,
            String eventType,
            String requestDescription) {

        EventRequest request = new EventRequest();

        request.setArtisan(artisan);

        request.setEventType(
                eventType == null
                        ? null
                        : eventType.trim().toUpperCase()
        );

        request.setRequestDescription(
                requestDescription == null
                        ? null
                        : requestDescription.trim()
        );

        request.setStatus("PENDING");

        return eventRequestRepository.save(request);
    }


    // =========================================================
    // GET ALL REQUESTS OF AN ARTISAN
    // =========================================================
    public List<EventRequest> getRequestsByArtisan(Long artisanId) {

        return eventRequestRepository
                .findByArtisanArtisanIdOrderByCreatedAtDesc(artisanId);
    }


    // =========================================================
    // GET PENDING REQUEST COUNT
    // =========================================================
    public long getPendingRequestCount(String eventType) {

        return eventRequestRepository
                .countByEventTypeAndStatusIgnoreCase(
                        eventType,
                        "PENDING"
                );
    }


    // =========================================================
    // GET PENDING REQUESTS
    // =========================================================
    public List<EventRequest> getPendingRequests(String eventType) {

        return eventRequestRepository
                .findByEventTypeAndStatusIgnoreCaseOrderByCreatedAtDesc(
                        eventType,
                        "PENDING"
                );
    }


    // =========================================================
    // UPDATE REQUEST STATUS
    // =========================================================
    public EventRequest updateStatus(
            Long requestId,
            String status) {

        EventRequest request =
                eventRequestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Event request not found: "
                                                + requestId
                                )
                        );

        request.setStatus(
                status == null
                        ? null
                        : status.trim().toUpperCase()
        );

        return eventRequestRepository.save(request);
    }
}