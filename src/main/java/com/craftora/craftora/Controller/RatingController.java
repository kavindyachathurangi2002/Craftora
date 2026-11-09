package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Customer;
import com.craftora.craftora.Entity.OrderItem;
import com.craftora.craftora.Entity.Product;
import com.craftora.craftora.Entity.Rating;
import com.craftora.craftora.Repository.CustomerRepository;
import com.craftora.craftora.Repository.ProductRepository;
import com.craftora.craftora.Service.RatingService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class RatingController {

    private final RatingService ratingService;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public RatingController(
            RatingService ratingService,
            CustomerRepository customerRepository,
            ProductRepository productRepository) {

        this.ratingService = ratingService;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    // =========================================================
    // RATINGS PAGE
    // =========================================================

    @GetMapping("/ratings")
    public String ratingsPage(Model model) {

        // Get all customer ratings from database
        List<Rating> ratings =
                ratingService.getAllRatings();

        // Overall rating information
        model.addAttribute(
                "ratings",
                ratings
        );

        model.addAttribute(
                "reviewCount",
                ratings.size()
        );

        model.addAttribute(
                "averageRating",
                ratingService.getOverallAverageRating()
        );

        // Rating breakdown
        model.addAttribute(
                "rating5Count",
                ratingService.getRatingCountByStars(5)
        );

        model.addAttribute(
                "rating4Count",
                ratingService.getRatingCountByStars(4)
        );

        model.addAttribute(
                "rating3Count",
                ratingService.getRatingCountByStars(3)
        );

        model.addAttribute(
                "rating2Count",
                ratingService.getRatingCountByStars(2)
        );

        model.addAttribute(
                "rating1Count",
                ratingService.getRatingCountByStars(1)
        );

        model.addAttribute(
                "rating5Percentage",
                ratingService.getRatingPercentage(5)
        );

        model.addAttribute(
                "rating4Percentage",
                ratingService.getRatingPercentage(4)
        );

        model.addAttribute(
                "rating3Percentage",
                ratingService.getRatingPercentage(3)
        );

        model.addAttribute(
                "rating2Percentage",
                ratingService.getRatingPercentage(2)
        );

        model.addAttribute(
                "rating1Percentage",
                ratingService.getRatingPercentage(1)
        );

        return "ratings";
    }

    // =========================================================
    // SUBMIT PRODUCT REVIEW
    // =========================================================

    @PostMapping("/ratings/submit/{productId}")
    public String submitRating(
            @PathVariable("productId") Long productId,
            @RequestParam("rating") Integer rating,
            @RequestParam(value = "review", required = false) String review,
            HttpSession session) {

        // -----------------------------------------------------
        // 1. Customer must be logged in
        // -----------------------------------------------------

        Object loggedInUserIdObject =
                session.getAttribute("loggedInUserId");

        if (loggedInUserIdObject == null) {
            return "redirect:/login";
        }

        Long userId;

        try {
            userId = Long.valueOf(
                    loggedInUserIdObject.toString()
            );
        } catch (Exception e) {
            return "redirect:/login";
        }

        // -----------------------------------------------------
        // 2. Find customer
        // -----------------------------------------------------

        Customer customer =
                customerRepository
                        .findByUserUserId(userId)
                        .orElse(null);

        if (customer == null) {
            return "redirect:/marketplace/product/"
                    + productId
                    + "?reviewError=customer";
        }

        // -----------------------------------------------------
        // 3. Find product
        // -----------------------------------------------------

        Product product =
                productRepository
                        .findById(productId)
                        .orElse(null);

        if (product == null) {
            return "redirect:/marketplace";
        }

        // -----------------------------------------------------
        // 4. Rating must be between 1 and 5
        // -----------------------------------------------------

        if (rating == null || rating < 1 || rating > 5) {
            return "redirect:/marketplace/product/"
                    + productId
                    + "?reviewError=rating";
        }

        // -----------------------------------------------------
        // 5. Customer can review only once
        // -----------------------------------------------------

        if (ratingService
                .getCustomerProductRating(
                        customer.getCustomerId(),
                        productId)
                .isPresent()) {

            return "redirect:/marketplace/product/"
                    + productId
                    + "?reviewError=already";
        }

        // -----------------------------------------------------
        // 6. Customer must have PAID for this product
        // -----------------------------------------------------

        OrderItem orderItem =
                ratingService
                        .getPurchasedOrderItem(
                                customer.getCustomerId(),
                                productId)
                        .orElse(null);

        if (orderItem == null) {
            return "redirect:/marketplace/product/"
                    + productId
                    + "?reviewError=notpurchased";
        }

        // -----------------------------------------------------
        // 7. Create rating
        // -----------------------------------------------------

        Rating newRating = new Rating();

        newRating.setCustomer(customer);
        newRating.setProduct(product);
        newRating.setOrderItem(orderItem);
        newRating.setRating(rating);

        if (review != null) {
            newRating.setReview(review.trim());
        }

        ratingService.saveRating(newRating);

        // -----------------------------------------------------
        // 8. Return to product page
        // -----------------------------------------------------

        return "redirect:/marketplace/product/"
                + productId
                + "?reviewSuccess=true";
    }
}