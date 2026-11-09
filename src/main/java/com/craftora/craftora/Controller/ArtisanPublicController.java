
package com.craftora.craftora.Controller;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Repository.ArtisanRepository;
import com.craftora.craftora.Service.ArtisanPaymentService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ArtisanPublicController {

    private final ArtisanRepository artisanRepository;
    private final ArtisanPaymentService artisanPaymentService;

    // Constructor
    public ArtisanPublicController(
            ArtisanRepository artisanRepository,
            ArtisanPaymentService artisanPaymentService) {

        this.artisanRepository = artisanRepository;
        this.artisanPaymentService = artisanPaymentService;
    }

    // ==========================================
    // DISPLAY ALL APPROVED AND PAID ARTISANS
    // ==========================================

    @GetMapping("/all-artisans")
    public String allArtisans(Model model) {

        List<Artisan> artisans = artisanRepository
                .findByStatusIgnoreCase("APPROVED")
                .stream()
                .filter(artisan -> {

                    if (artisan.getUser() == null) {
                        return false;
                    }

                    Long userId = artisan.getUser().getUserId();

                    // Admin artisan (User ID 10) does not need payment
                    if (Objects.equals(userId, 10L)) {
                        return true;
                    }

                    // Other artisans must complete registration payment
                    return artisanPaymentService.isPaymentCompleted(userId);
                })
                .collect(Collectors.toList());

        model.addAttribute("artisans", artisans);

        return "all-artisans";
    }


    // ==========================================
    // VIEW INDIVIDUAL ARTISAN PROFILE
    // ==========================================

    @GetMapping("/artisan-profile/{artisanId}")
    public String viewArtisanProfile(
            @PathVariable Long artisanId,
            HttpSession session,
            Model model) {

        // Check whether user is logged in
        if (session.getAttribute("loggedInUser") == null) {
            return "artisan-signin-required";
        }

        // Find artisan by ID
        Artisan artisan = artisanRepository.findById(artisanId)
                .orElse(null);

        // Check whether artisan exists
        if (artisan == null) {
            return "redirect:/all-artisans";
        }

        // Only approved artisans can be viewed
        if (!"APPROVED".equalsIgnoreCase(artisan.getStatus())) {
            return "redirect:/all-artisans";
        }

        // Check whether artisan has a linked user
        if (artisan.getUser() == null) {
            return "redirect:/all-artisans";
        }

        Long userId = artisan.getUser().getUserId();

        // Admin artisan does not need payment
        boolean isAdminArtisan = Objects.equals(userId, 10L);

        // Other artisans must have completed payment
        if (!isAdminArtisan
                && !artisanPaymentService.isPaymentCompleted(userId)) {

            return "redirect:/all-artisans";
        }

        // Send artisan details to HTML page
        model.addAttribute("artisan", artisan);

        return "artisan-public-profile";
    }

}