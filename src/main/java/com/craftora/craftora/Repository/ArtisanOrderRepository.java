package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Entity.ArtisanOrder;
import com.craftora.craftora.Entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtisanOrderRepository extends JpaRepository<ArtisanOrder, Long> {

    List<ArtisanOrder> findByArtisanOrderByCreatedAtDesc(Artisan artisan);

    List<ArtisanOrder> findByOrder(Order order);

    List<ArtisanOrder> findByOrderOrderId(Long orderId);

    List<ArtisanOrder> findByArtisanAndOrderStatusOrderByCreatedAtDesc(
            Artisan artisan,
            String orderStatus
    );
}