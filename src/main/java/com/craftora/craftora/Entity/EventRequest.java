package com.craftora.craftora.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_requests")
public class EventRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @ManyToOne
    @JoinColumn(name = "artisan_id", nullable = false)
    private Artisan artisan;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "request_description", columnDefinition = "TEXT")
    private String requestDescription;

    @Column(name = "status", length = 20)
    private String status = "PENDING";

   @Column(name = "created_at", nullable = false)
private LocalDateTime createdAt;


    // =========================
    // GETTERS
    // =========================

    public Long getRequestId() {
        return requestId;
    }

    public Artisan getArtisan() {
        return artisan;
    }

    public String getEventType() {
        return eventType;
    }

    public String getRequestDescription() {
        return requestDescription;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


    // =========================
    // SETTERS
    // =========================

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public void setArtisan(Artisan artisan) {
        this.artisan = artisan;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setRequestDescription(String requestDescription) {
        this.requestDescription = requestDescription;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
protected void onCreate() {
    if (createdAt == null) {
        createdAt = LocalDateTime.now();
    }

    if (status == null || status.trim().isEmpty()) {
        status = "PENDING";
    }
}
}