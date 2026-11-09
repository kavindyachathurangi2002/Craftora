package com.craftora.craftora.Service;

import com.craftora.craftora.Entity.ArtisanRequest;
import com.craftora.craftora.Entity.ArtisanPayment;
import com.craftora.craftora.Entity.OrderPayment;

import com.craftora.craftora.Repository.ArtisanRepository;
import com.craftora.craftora.Repository.ArtisanRequestRepository;
import com.craftora.craftora.Repository.EventRepository;
import com.craftora.craftora.Repository.EventPaymentRepository;
import com.craftora.craftora.Repository.ArtisanPaymentRepository;
import com.craftora.craftora.Repository.OrderPaymentRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
public class AdminService {

    private final ArtisanRepository artisanRepository;

    private final ArtisanRequestRepository artisanRequestRepository;
    private final EventRepository eventRepository;
    private final ArtisanPaymentRepository artisanPaymentRepository;
    private final OrderPaymentRepository orderPaymentRepository;

    public AdminService(
            ArtisanRepository artisanRepository,
            ArtisanRequestRepository artisanRequestRepository,
            EventRepository eventRepository,
            EventPaymentRepository eventPaymentRepository,
            ArtisanPaymentRepository artisanPaymentRepository,
            OrderPaymentRepository orderPaymentRepository) {

        this.artisanRepository = artisanRepository;
        this.artisanRequestRepository = artisanRequestRepository;
        this.eventRepository = eventRepository;
        this.artisanPaymentRepository = artisanPaymentRepository;
        this.orderPaymentRepository = orderPaymentRepository;
    }


    // ==============================
    // APPROVED ARTISANS COUNT
    // ==============================

    public long getApprovedArtisanCount() {

        return artisanRepository
                .findByStatusIgnoreCase("APPROVED")
                .size();
    }


    // ==============================
    // TOTAL EVENTS COUNT
    // ==============================

    public long getEventCount() {

        return eventRepository.count();
    }


    // ==============================
    // TOTAL REVENUE
    // ==============================

    public BigDecimal getTotalRevenue() {

        BigDecimal registrationFees = BigDecimal.ZERO;

        BigDecimal productSales = BigDecimal.ZERO;


        // ==========================================
        // 1. PAID ARTISAN REGISTRATION FEES
        // ==========================================

        List<ArtisanPayment> artisanPayments =
                artisanPaymentRepository
                        .findByPaymentStatusIgnoreCase("PAID");

        for (ArtisanPayment payment : artisanPayments) {

            if (payment.getAmount() != null) {

                registrationFees =
                        registrationFees.add(
                                payment.getAmount()
                        );
            }
        }


        // ==========================================
        // 2. PAID PRODUCT SALES
        // ==========================================

        List<OrderPayment> orderPayments =
                orderPaymentRepository
                        .findByPaymentStatusIgnoreCase("PAID");

        for (OrderPayment payment : orderPayments) {

            if (payment.getAmount() != null) {

                productSales =
                        productSales.add(
                                payment.getAmount()
                        );
            }
        }


        // ==========================================
        // 3. CRAFTORA 3% COMMISSION
        // ==========================================

        BigDecimal productCommission =
                productSales
                        .multiply(new BigDecimal("0.03"))
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );


        // ==========================================
        // 4. ACTUAL CRAFTORA REVENUE
        //
        // Registration Fees
        // +
        // 3% of Paid Product Sales
        // ==========================================

        return registrationFees.add(
                productCommission
        );
    }


    // ==============================
    // RECENT ARTISAN REQUESTS
    // ==============================

    public List<ArtisanRequest> getRecentArtisanRequests() {

        List<ArtisanRequest> requests =
                artisanRequestRepository.findAll();

        return requests.stream()
                .sorted(
                        Comparator.comparing(
                                ArtisanRequest::getRequestedAt,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
                )
                .limit(5)
                .toList();
    }


    // ==============================
    // PENDING ARTISAN REQUEST COUNT
    // ==============================

    public long getPendingArtisanRequestCount() {

        return artisanRequestRepository
                .findByStatus("PENDING")
                .size();
    }
}