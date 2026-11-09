package com.craftora.craftora.Service;

import com.craftora.craftora.Entity.OrderItem;
import com.craftora.craftora.Entity.Rating;
import com.craftora.craftora.Repository.OrderItemRepository;
import com.craftora.craftora.Repository.RatingRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final OrderItemRepository orderItemRepository;

    public RatingService(
            RatingRepository ratingRepository,
            OrderItemRepository orderItemRepository) {

        this.ratingRepository = ratingRepository;
        this.orderItemRepository = orderItemRepository;
    }

    // =========================================================
    // SAVE RATING
    // =========================================================

    public Rating saveRating(Rating rating) {

        return ratingRepository.save(rating);
    }

    // =========================================================
    // GET RATINGS BY PRODUCT
    // =========================================================

    public List<Rating> getRatingsByProduct(Long productId) {

        return ratingRepository
                .findByProductProductIdOrderByCreatedAtDesc(productId);
    }

    // =========================================================
    // GET ALL RATINGS
    // =========================================================

    public List<Rating> getAllRatings() {

        List<Rating> ratings =
                ratingRepository.findAll();

        ratings.sort(
                (r1, r2) -> {

                    if (r1.getCreatedAt() == null
                            && r2.getCreatedAt() == null) {
                        return 0;
                    }

                    if (r1.getCreatedAt() == null) {
                        return 1;
                    }

                    if (r2.getCreatedAt() == null) {
                        return -1;
                    }

                    return r2.getCreatedAt()
                            .compareTo(r1.getCreatedAt());
                }
        );

        return ratings;
    }

    // =========================================================
    // GET CUSTOMER PRODUCT RATING
    // =========================================================

    public Optional<Rating> getCustomerProductRating(
            Long customerId,
            Long productId) {

        return ratingRepository
                .findByCustomerCustomerIdAndProductProductId(
                        customerId,
                        productId
                );
    }

    // =========================================================
    // PRODUCT REVIEW COUNT
    // =========================================================

    public long getReviewCount(Long productId) {

        return ratingRepository
                .countByProductProductId(productId);
    }

    // =========================================================
    // PRODUCT AVERAGE RATING
    // =========================================================

    public Double getAverageRating(Long productId) {

        Double average =
                ratingRepository
                        .findAverageRatingByProductProductId(
                                productId
                        );

        return average != null ? average : 0.0;
    }

    // =========================================================
    // OVERALL AVERAGE RATING
    // =========================================================

    public double getOverallAverageRating() {

        List<Rating> ratings =
                ratingRepository.findAll();

        if (ratings.isEmpty()) {
            return 0.0;
        }

        double total = 0;

        for (Rating rating : ratings) {

            if (rating.getRating() != null) {
                total += rating.getRating();
            }
        }

        return Math.round(
                (total / ratings.size()) * 10.0
        ) / 10.0;
    }

    // =========================================================
    // GET RATING COUNT BY STARS
    // =========================================================

    public long getRatingCountByStars(int stars) {

        List<Rating> ratings =
                ratingRepository.findAll();

        long count = 0;

        for (Rating rating : ratings) {

            if (rating.getRating() != null
                    && rating.getRating() == stars) {

                count++;
            }
        }

        return count;
    }

    // =========================================================
    // GET RATING PERCENTAGE
    // =========================================================

    public int getRatingPercentage(int stars) {

        List<Rating> ratings =
                ratingRepository.findAll();

        if (ratings.isEmpty()) {
            return 0;
        }

        long count = 0;

        for (Rating rating : ratings) {

            if (rating.getRating() != null
                    && rating.getRating() == stars) {

                count++;
            }
        }

        return (int) Math.round(
                (count * 100.0) / ratings.size()
        );
    }

    // =========================================================
    // CHECK WHETHER CUSTOMER PURCHASED THIS PRODUCT
    // =========================================================

    public Optional<OrderItem> getPurchasedOrderItem(
            Long customerId,
            Long productId) {

        return orderItemRepository
                .findFirstByOrder_Customer_CustomerIdAndProduct_ProductIdAndOrder_PaymentStatusIgnoreCase(
                        customerId,
                        productId,
                        "PAID"
                );
    }
}