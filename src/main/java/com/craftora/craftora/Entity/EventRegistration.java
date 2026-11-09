
package com.craftora.craftora.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_registrations")
public class EventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registration_id")
    private Long registrationId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "registration_role", nullable = false)
    private String registrationRole;

    @Column(name = "registration_status")
    private String registrationStatus = "REGISTERED";

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "NOT_REQUIRED";

    @Column(name = "attendance_status", nullable = false)
    private String attendanceStatus = "NOT_MARKED";

    @Column(name = "attendance_marked_at")
    private LocalDateTime attendanceMarkedAt;

    @Column(name = "certificate_available", nullable = false)
    private Boolean certificateAvailable = false;

    @Column(name = "certificate_id")
    private String certificateId;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    public EventRegistration() {
    }

    public Long getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Long registrationId) {
        this.registrationId = registrationId;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRegistrationRole() {
        return registrationRole;
    }

    public void setRegistrationRole(String registrationRole) {
        this.registrationRole = registrationRole;
    }

    public String getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(String registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(String attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public LocalDateTime getAttendanceMarkedAt() {
        return attendanceMarkedAt;
    }

    public void setAttendanceMarkedAt(
            LocalDateTime attendanceMarkedAt) {
        this.attendanceMarkedAt = attendanceMarkedAt;
    }

    public Boolean getCertificateAvailable() {
        return certificateAvailable;
    }

    public void setCertificateAvailable(
            Boolean certificateAvailable) {
        this.certificateAvailable = certificateAvailable;
    }

    public String getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(String certificateId) {
        this.certificateId = certificateId;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    @PrePersist
    protected void onCreate() {

        if (registeredAt == null) {
            registeredAt = LocalDateTime.now();
        }

        if (registrationStatus == null
                || registrationStatus.trim().isEmpty()) {
            registrationStatus = "REGISTERED";
        }

        if (paymentStatus == null
                || paymentStatus.trim().isEmpty()) {
            paymentStatus = "NOT_REQUIRED";
        }

        if (attendanceStatus == null
                || attendanceStatus.trim().isEmpty()) {
            attendanceStatus = "NOT_MARKED";
        }

        if (certificateAvailable == null) {
            certificateAvailable = false;
        }
    }
}
