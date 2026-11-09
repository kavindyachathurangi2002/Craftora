package com.craftora.craftora.Service;

import com.craftora.craftora.Entity.ArtisanPayment;
import com.craftora.craftora.Repository.ArtisanPaymentRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArtisanPaymentService {

    private final ArtisanPaymentRepository artisanPaymentRepository;

    public ArtisanPaymentService(
            ArtisanPaymentRepository artisanPaymentRepository) {

        this.artisanPaymentRepository = artisanPaymentRepository;
    }

    // =====================================================
    // CHECK WHETHER ARTISAN REGISTRATION FEE IS PAID
    // =====================================================

    public boolean isPaymentCompleted(Long userId) {

        List<ArtisanPayment> payments =
                artisanPaymentRepository.findByUserId(userId);

        return payments.stream()
                .anyMatch(payment ->
                        "PAID".equalsIgnoreCase(
                                payment.getPaymentStatus()
                        )
                );
    }
}