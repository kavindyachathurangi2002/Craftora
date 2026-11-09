package com.craftora.craftora.Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "artisan_id")
    private Long artisanId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "event_title")
    private String eventTitle;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "location")
    private String location;

    // =====================================================
    // EVENT LOCATION COORDINATES
    // =====================================================

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    // =====================================================
    // EVENT REGISTRATION SETTINGS
    // =====================================================

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "registration_deadline")
    private LocalDateTime registrationDeadline;

    @Column(name = "allow_customers", nullable = false)
    private Boolean allowCustomers = true;

    @Column(name = "allow_artisans", nullable = false)
    private Boolean allowArtisans = true;

    @Column(name = "customer_registration_fee", nullable = false)
    private BigDecimal customerRegistrationFee = BigDecimal.ZERO;

    @Column(name = "artisan_registration_fee", nullable = false)
    private BigDecimal artisanRegistrationFee = BigDecimal.ZERO;

    // =====================================================
    // EVENT RULES & IMAGE
    // =====================================================

    @Column(name = "rules", columnDefinition = "TEXT")
    private String rules;

    @Column(name = "event_image")
    private String eventImage;

    // =====================================================
    // EVENT STATUS
    // =====================================================

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public Event() {
    }


    // =====================================================
    // EVENT ID
    // =====================================================

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }


    // =====================================================
    // REQUEST ID
    // =====================================================

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }


    // =====================================================
    // ARTISAN ID
    // =====================================================

    public Long getArtisanId() {
        return artisanId;
    }

    public void setArtisanId(Long artisanId) {
        this.artisanId = artisanId;
    }


    // =====================================================
    // EVENT TYPE
    // =====================================================

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }


    // =====================================================
    // EVENT TITLE
    // =====================================================

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }


    // =====================================================
    // DESCRIPTION
    // =====================================================

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    // =====================================================
    // EVENT DATE
    // =====================================================

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }


    // =====================================================
    // START TIME
    // =====================================================

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }


    // =====================================================
    // END TIME
    // =====================================================

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }


    // =====================================================
    // LOCATION
    // =====================================================

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }


    // =====================================================
    // LATITUDE
    // =====================================================

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }


    // =====================================================
    // LONGITUDE
    // =====================================================

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }


    // =====================================================
    // CAPACITY
    // =====================================================

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }


    // =====================================================
    // REGISTRATION DEADLINE
    // =====================================================

    public LocalDateTime getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(
            LocalDateTime registrationDeadline) {

        this.registrationDeadline = registrationDeadline;
    }


    // =====================================================
    // ALLOW CUSTOMERS
    // =====================================================

    public Boolean getAllowCustomers() {
        return allowCustomers;
    }

    public void setAllowCustomers(Boolean allowCustomers) {
        this.allowCustomers = allowCustomers;
    }


    // =====================================================
    // ALLOW ARTISANS
    // =====================================================

    public Boolean getAllowArtisans() {
        return allowArtisans;
    }

    public void setAllowArtisans(Boolean allowArtisans) {
        this.allowArtisans = allowArtisans;
    }


    // =====================================================
    // CUSTOMER REGISTRATION FEE
    // =====================================================

    public BigDecimal getCustomerRegistrationFee() {
        return customerRegistrationFee;
    }

    public void setCustomerRegistrationFee(
            BigDecimal customerRegistrationFee) {

        this.customerRegistrationFee =
                customerRegistrationFee;
    }


    // =====================================================
    // ARTISAN REGISTRATION FEE
    // =====================================================

    public BigDecimal getArtisanRegistrationFee() {
        return artisanRegistrationFee;
    }

    public void setArtisanRegistrationFee(
            BigDecimal artisanRegistrationFee) {

        this.artisanRegistrationFee =
                artisanRegistrationFee;
    }


    // =====================================================
    // RULES
    // =====================================================

    public String getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }


    // =====================================================
    // EVENT IMAGE
    // =====================================================

    public String getEventImage() {
        return eventImage;
    }

    public void setEventImage(String eventImage) {
        this.eventImage = eventImage;
    }


    // =====================================================
    // STATUS
    // =====================================================

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    // =====================================================
    // CREATED AT
    // =====================================================

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


    // =====================================================
    // UPDATED AT
    // =====================================================

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}