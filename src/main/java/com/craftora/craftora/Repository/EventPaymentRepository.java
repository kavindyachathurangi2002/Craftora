package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.EventPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventPaymentRepository
        extends JpaRepository<EventPayment, Long> {

    List<EventPayment> findByPaymentStatusIgnoreCase(String paymentStatus);

    List<EventPayment> findByEventId(Long eventId);

    List<EventPayment> findByUserId(Long userId);

    List<EventPayment> findByRegistrationId(Long registrationId);

    Optional<EventPayment> findByTransactionId(String transactionId);
}