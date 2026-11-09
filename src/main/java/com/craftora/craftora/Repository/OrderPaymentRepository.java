package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.OrderPayment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderPaymentRepository
        extends JpaRepository<OrderPayment, Long> {

    List<OrderPayment>
    findByPaymentStatusIgnoreCase(
            String paymentStatus
    );

    List<OrderPayment>
    findByUserId(Long userId);

    List<OrderPayment>
    findByOrderId(Long orderId);

    Optional<OrderPayment>
    findByTransactionId(
            String transactionId
    );
}