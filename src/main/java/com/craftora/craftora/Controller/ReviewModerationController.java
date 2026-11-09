
package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.*;
import com.craftora.craftora.Repository.*;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ReviewModerationController {

    private final ArtisanRepository artisanRepository;
    private final ProductRepository productRepository;
    private final RatingRepository ratingRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final UserRepository userRepository;

    public ReviewModerationController(
            ArtisanRepository artisanRepository,
            ProductRepository productRepository,
            RatingRepository ratingRepository,
            ReviewReportRepository reviewReportRepository,
            UserRepository userRepository) {

        this.artisanRepository = artisanRepository;
        this.productRepository = productRepository;
        this.ratingRepository = ratingRepository;
        this.reviewReportRepository = reviewReportRepository;
        this.userRepository = userRepository;
    }

    // =========================================
    // GET LOGGED-IN USER
    // =========================================

    private User getLoggedInUser(HttpSession session) {

        Object userIdObject =
                session.getAttribute("loggedInUserId");

        if (userIdObject == null) {
            return null;
        }

        try {
            Long userId =
                    Long.valueOf(userIdObject.toString());

            return userRepository.findById(userId)
                    .orElse(null);

        } catch (Exception e) {
            return null;
        }
    }

    // =========================================
    // CHECK ADMIN
    // =========================================

    private boolean isAdmin(User user) {

        if (user == null || user.getRoles() == null) {
            return false;
        }

        return user.getRoles()
                .stream()
                .anyMatch(role ->
                        role.getRoleName()
                                .equalsIgnoreCase("ADMIN"));
    }

    // =========================================
    // ARTISAN REVIEWS PAGE
    // =========================================

    @GetMapping("/artisan/reviews")
    public String artisanReviews(
            HttpSession session,
            Model model) {

        User user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        Artisan artisan =
                artisanRepository
                        .findByUserUserId(user.getUserId())
                        .orElse(null);

        if (artisan == null) {
            return "redirect:/login";
        }

        List<Product> products =
                productRepository.findByArtisan(artisan);

        List<Rating> ratings = new ArrayList<>();

        for (Product product : products) {

            List<Rating> productRatings =
                    ratingRepository
                            .findByProductProductIdOrderByCreatedAtDesc(
                                    product.getProductId());

            ratings.addAll(productRatings);
        }

        model.addAttribute("artisan", artisan);
        model.addAttribute("ratings", ratings);

        return "artisan-reviews";
    }

    // =========================================
    // REPORT REVIEW
    // =========================================

    @PostMapping("/artisan/reviews/report")
    public String reportReview(
            @RequestParam("ratingId") Long ratingId,
            @RequestParam("reason") String reason,
            HttpSession session) {

        User user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        Artisan artisan =
                artisanRepository
                        .findByUserUserId(user.getUserId())
                        .orElse(null);

        if (artisan == null) {
            return "redirect:/login";
        }

        Rating rating =
                ratingRepository.findById(ratingId)
                        .orElse(null);

        // Check whether the review belongs to this artisan
        if (rating == null ||
                rating.getProduct() == null ||
                rating.getProduct().getArtisan() == null ||
                !rating.getProduct().getArtisan()
                        .getArtisanId()
                        .equals(artisan.getArtisanId())) {

            return "redirect:/artisan/reviews?error=invalid";
        }

        // Check report reason
        if (reason == null || reason.trim().isEmpty()) {
            return "redirect:/artisan/reviews?error=reason";
        }

        // Find an existing report for this review
        ReviewReport existingReport =
                reviewReportRepository
                        .findByRatingRatingId(rating.getRatingId())
                        .orElse(null);

        // If a report already exists
        if (existingReport != null) {

            // Do not allow another report while pending
            if ("PENDING".equals(existingReport.getStatus())) {

                return "redirect:/artisan/reviews?message=alreadyReported";
            }

            // Reopen a dismissed report
            existingReport.setReason(reason.trim());
            existingReport.setStatus("PENDING");

            reviewReportRepository.save(existingReport);

            return "redirect:/artisan/reviews?success=reported";
        }

        // Create a new report if no previous report exists
        ReviewReport report = new ReviewReport();

        report.setRating(rating);
        report.setReason(reason.trim());
        report.setStatus("PENDING");

        reviewReportRepository.save(report);

        return "redirect:/artisan/reviews?success=reported";
    }

    // =========================================
    // ADMIN REPORTED REVIEWS
    // =========================================

    @GetMapping("/admin/reported-reviews")
    public String reportedReviews(
            HttpSession session,
            Model model) {

        User user = getLoggedInUser(session);

        if (!isAdmin(user)) {
            return "redirect:/login";
        }

        List<ReviewReport> reports =
                reviewReportRepository
                        .findByStatusOrderByCreatedAtDesc(
                                "PENDING");

        model.addAttribute("reports", reports);
        model.addAttribute("user", user);

        return "admin-reported-reviews";
    }

    // =========================================
    // DISMISS REPORT
    // =========================================

    @PostMapping("/admin/reported-reviews/dismiss")
    @Transactional
    public String dismissReport(
            @RequestParam("reportId") Long reportId,
            HttpSession session) {

        User user = getLoggedInUser(session);

        if (!isAdmin(user)) {
            return "redirect:/login";
        }

        ReviewReport report =
                reviewReportRepository.findById(reportId)
                        .orElse(null);

        if (report != null &&
                "PENDING".equals(report.getStatus())) {

            report.setStatus("DISMISSED");

            reviewReportRepository.save(report);
        }

        return "redirect:/admin/reported-reviews";
    }

    // =========================================
    // DELETE REPORTED REVIEW
    // =========================================

    @PostMapping("/admin/reported-reviews/delete")
    @Transactional
    public String deleteReportedReview(
            @RequestParam("reportId") Long reportId,
            HttpSession session) {

        User user = getLoggedInUser(session);

        if (!isAdmin(user)) {
            return "redirect:/login";
        }

        ReviewReport report =
                reviewReportRepository.findById(reportId)
                        .orElse(null);

        if (report == null) {
            return "redirect:/admin/reported-reviews";
        }

        Rating rating = report.getRating();

        // Delete the report first
        reviewReportRepository.delete(report);

        // Then delete the customer review
        if (rating != null) {
            ratingRepository.delete(rating);
        }

        return "redirect:/admin/reported-reviews?deleted=success";
    }
}