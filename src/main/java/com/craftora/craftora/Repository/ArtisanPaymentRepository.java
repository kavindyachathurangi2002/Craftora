package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.ArtisanPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtisanPaymentRepository
        extends JpaRepository<ArtisanPayment, Long> {

    List<ArtisanPayment> findByPaymentStatusIgnoreCase(String paymentStatus);

    List<ArtisanPayment> findByUserId(Long userId);

    List<ArtisanPayment> findByRequestId(Long requestId);

    Optional<ArtisanPayment> findByTransactionId(String transactionId);

    Optional<ArtisanPayment>
findFirstByRequestIdAndPaymentStatusIgnoreCaseOrderByCreatedAtDesc(
        Long requestId,
        String paymentStatus
);
}