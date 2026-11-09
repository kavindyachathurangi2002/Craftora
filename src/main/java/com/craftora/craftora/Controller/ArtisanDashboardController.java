package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Entity.ArtisanOrder;
import com.craftora.craftora.Entity.Product;
import com.craftora.craftora.Entity.DiscussionRoom;
import com.craftora.craftora.Entity.Event;

import com.craftora.craftora.Repository.ArtisanOrderRepository;
import com.craftora.craftora.Repository.ArtisanRepository;
import com.craftora.craftora.Repository.ProductRepository;
import com.craftora.craftora.Repository.DiscussionRoomRepository;
import com.craftora.craftora.Repository.EventRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class ArtisanDashboardController {

    private final ArtisanRepository artisanRepository;

    private final ArtisanOrderRepository artisanOrderRepository;

    private final ProductRepository productRepository;

    private final DiscussionRoomRepository discussionRoomRepository;

    private final EventRepository eventRepository;


    public ArtisanDashboardController(
            ArtisanRepository artisanRepository,
            ArtisanOrderRepository artisanOrderRepository,
            ProductRepository productRepository,
            DiscussionRoomRepository discussionRoomRepository,
            EventRepository eventRepository) {

        this.artisanRepository = artisanRepository;
        this.artisanOrderRepository = artisanOrderRepository;
        this.productRepository = productRepository;
        this.discussionRoomRepository = discussionRoomRepository;
        this.eventRepository = eventRepository;
    }


    @GetMapping("/artisan-dashboard")
    public String artisanDashboard(
            HttpSession session,
            Model model) {

        // =====================================================
        // CHECK LOGIN
        // =====================================================

        Object loggedInUserId =
                session.getAttribute("loggedInUserId");

        if (loggedInUserId == null) {
            return "redirect:/login";
        }

        Long userId =
                Long.valueOf(loggedInUserId.toString());


        // =====================================================
        // FIND LOGGED-IN ARTISAN
        // =====================================================

        Artisan artisan =
                artisanRepository
                        .findByUserUserId(userId)
                        .orElse(null);

        if (artisan == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "artisan",
                artisan
        );


        // =====================================================
        // PRODUCTS
        // =====================================================

        List<Product> allProducts =
                productRepository.findAll();

        int productCount = 0;

        for (Product product : allProducts) {

            if (product.getArtisan() != null
                    && product.getArtisan().getArtisanId() != null
                    && artisan.getArtisanId() != null
                    && product.getArtisan()
                            .getArtisanId()
                            .equals(artisan.getArtisanId())) {

                productCount++;
            }
        }

        model.addAttribute(
                "productCount",
                productCount
        );


        // =====================================================
        // ARTISAN ORDERS
        // =====================================================

        List<ArtisanOrder> artisanOrders =
                artisanOrderRepository
                        .findByArtisanOrderByCreatedAtDesc(
                                artisan
                        );

        int orderCount =
                artisanOrders.size();

        model.addAttribute(
                "orderCount",
                orderCount
        );


        // =====================================================
        // NEW ORDERS
        // =====================================================

        List<ArtisanOrder> newOrders =
                artisanOrderRepository
                        .findByArtisanAndOrderStatusOrderByCreatedAtDesc(
                                artisan,
                                "NEW"
                        );

        int newOrderCount =
                newOrders.size();

        model.addAttribute(
                "newOrderCount",
                newOrderCount
        );


        // =====================================================
        // ARTISAN PAYMENTS / EARNINGS
        // =====================================================

        BigDecimal totalPayments =
                BigDecimal.ZERO;

        for (ArtisanOrder artisanOrder :
                artisanOrders) {

            if (artisanOrder.getArtisanAmount() != null) {

                totalPayments =
                        totalPayments.add(
                                artisanOrder.getArtisanAmount()
                        );
            }
        }

        model.addAttribute(
                "totalPayments",
                totalPayments
        );


        // =====================================================
        // LIVE EVENT DISCUSSIONS
        // =====================================================

        DiscussionRoom workshopOpenDiscussion =
                discussionRoomRepository
                        .findFirstByEventTypeIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
                                "WORKSHOP",
                                "OPEN"
                        )
                        .orElse(null);

        DiscussionRoom exhibitionOpenDiscussion =
                discussionRoomRepository
                        .findFirstByEventTypeIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
                                "EXHIBITION",
                                "OPEN"
                        )
                        .orElse(null);

        DiscussionRoom competitionOpenDiscussion =
                discussionRoomRepository
                        .findFirstByEventTypeIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
                                "COMPETITION",
                                "OPEN"
                        )
                        .orElse(null);


        model.addAttribute(
                "workshopOpenDiscussion",
                workshopOpenDiscussion
        );

        model.addAttribute(
                "exhibitionOpenDiscussion",
                exhibitionOpenDiscussion
        );

        model.addAttribute(
                "competitionOpenDiscussion",
                competitionOpenDiscussion
        );


        // =====================================================
        // RETURN DASHBOARD
        // =====================================================

        return "artisan-dashboard";
    }


    // =========================================================
    // ARTISAN UPCOMING EVENTS
    // =========================================================

    @GetMapping("/artisan/events/upcoming")
    public String artisanUpcomingEvents(
            HttpSession session,
            Model model) {

        // -----------------------------------------------------
        // CHECK LOGIN
        // -----------------------------------------------------

        Object loggedInUserId =
                session.getAttribute("loggedInUserId");

        if (loggedInUserId == null) {
            return "redirect:/login";
        }


        // -----------------------------------------------------
        // GET UPCOMING EVENTS AVAILABLE FOR ARTISANS
        // -----------------------------------------------------

        List<Event> events =
                eventRepository
                        .findByStatusIgnoreCaseAndAllowArtisansTrueAndEventDateGreaterThanEqualOrderByEventDateAsc(
                                "PUBLISHED",
                                LocalDate.now()
                        );


        // -----------------------------------------------------
        // SEND EVENTS TO PAGE
        // -----------------------------------------------------

        model.addAttribute(
                "events",
                events
        );


        // -----------------------------------------------------
        // OPEN ARTISAN UPCOMING EVENTS PAGE
        // -----------------------------------------------------

        return "artisan-upcoming-events";
    }
}