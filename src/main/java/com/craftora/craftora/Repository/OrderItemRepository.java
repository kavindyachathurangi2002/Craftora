package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.Order;
import com.craftora.craftora.Entity.OrderItem;
import com.craftora.craftora.Entity.Artisan;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder(Order order);

    List<OrderItem> findByArtisanOrderByOrder_OrderDateDesc(Artisan artisan);

    Optional<OrderItem> findFirstByOrder_Customer_CustomerIdAndProduct_ProductIdAndOrder_PaymentStatusIgnoreCase(
        Long customerId,
        Long productId,
        String paymentStatus
);
}


