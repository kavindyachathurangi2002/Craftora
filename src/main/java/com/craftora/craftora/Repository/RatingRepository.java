package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByProductProductIdOrderByCreatedAtDesc(Long productId);

    Optional<Rating> findByCustomerCustomerIdAndProductProductId(
            Long customerId,
            Long productId
    );

    long countByProductProductId(Long productId);

    @Query("""
            SELECT AVG(r.rating)
            FROM Rating r
            WHERE r.product.productId = :productId
            """)
    Double findAverageRatingByProductProductId(
            @Param("productId") Long productId
    );
}