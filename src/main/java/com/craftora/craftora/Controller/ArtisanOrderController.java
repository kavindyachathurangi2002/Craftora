package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Entity.ArtisanOrder;
import com.craftora.craftora.Entity.OrderItem;
import com.craftora.craftora.Repository.ArtisanOrderRepository;
import com.craftora.craftora.Repository.ArtisanRepository;
import com.craftora.craftora.Repository.OrderItemRepository;
import com.craftora.craftora.Service.OrderStatusEmailService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ArtisanOrderController {

    private final ArtisanRepository artisanRepository;
    private final ArtisanOrderRepository artisanOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusEmailService orderStatusEmailService;

    public ArtisanOrderController(
            ArtisanRepository artisanRepository,
            ArtisanOrderRepository artisanOrderRepository,
            OrderItemRepository orderItemRepository,
            OrderStatusEmailService orderStatusEmailService) {

        this.artisanRepository = artisanRepository;
        this.artisanOrderRepository = artisanOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderStatusEmailService = orderStatusEmailService;
    }

    // =========================================================
    // ARTISAN ORDERS
    // =========================================================

    @GetMapping("/artisan/orders")
    public String artisanOrders(
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

        Long userId =
                Long.valueOf(loggedInUserId.toString());

        // -----------------------------------------------------
        // FIND LOGGED-IN ARTISAN
        // -----------------------------------------------------

        Artisan artisan =
                artisanRepository
                        .findByUserUserId(userId)
                        .orElse(null);

        if (artisan == null) {
            return "redirect:/artisan-dashboard";
        }

        // -----------------------------------------------------
        // GET ONLY THIS ARTISAN'S ORDERS
        // -----------------------------------------------------

        List<ArtisanOrder> artisanOrders =
                artisanOrderRepository
                        .findByArtisanOrderByCreatedAtDesc(artisan);

        // -----------------------------------------------------
        // CREATE ORDER VIEWS
        // -----------------------------------------------------

        List<ArtisanOrderView> orderViews =
                new ArrayList<>();

        for (ArtisanOrder artisanOrder : artisanOrders) {

            List<OrderItem> allItems =
                    orderItemRepository
                            .findByOrder(artisanOrder.getOrder());

            List<OrderItem> artisanItems =
                    new ArrayList<>();

            for (OrderItem item : allItems) {

                if (item.getArtisan() != null
                        && artisan.getArtisanId() != null
                        && item.getArtisan().getArtisanId() != null
                        && item.getArtisan()
                                .getArtisanId()
                                .equals(artisan.getArtisanId())) {

                    artisanItems.add(item);
                }
            }

            orderViews.add(
                    new ArtisanOrderView(
                            artisanOrder,
                            artisanItems
                    )
            );
        }

        model.addAttribute(
                "artisan",
                artisan
        );

        model.addAttribute(
                "orderViews",
                orderViews
        );

        return "artisan-orders";
    }

    // =========================================================
    // UPDATE ORDER STATUS
    // =========================================================

    @PostMapping("/artisan/orders/update-status")
    public String updateOrderStatus(
            @RequestParam("artisanOrderId") Long artisanOrderId,
            @RequestParam("status") String status,
            HttpSession session) {

        // -----------------------------------------------------
        // CHECK LOGIN
        // -----------------------------------------------------

        Object loggedInUserId =
                session.getAttribute("loggedInUserId");

        if (loggedInUserId == null) {
            return "redirect:/login";
        }

        Long userId =
                Long.valueOf(loggedInUserId.toString());

        // -----------------------------------------------------
        // FIND LOGGED-IN ARTISAN
        // -----------------------------------------------------

        Artisan artisan =
                artisanRepository
                        .findByUserUserId(userId)
                        .orElse(null);

        if (artisan == null) {
            return "redirect:/artisan-dashboard";
        }

        // -----------------------------------------------------
        // FIND ARTISAN ORDER
        // -----------------------------------------------------

        ArtisanOrder artisanOrder =
                artisanOrderRepository
                        .findById(artisanOrderId)
                        .orElse(null);

        if (artisanOrder == null) {
            return "redirect:/artisan/orders";
        }

        // -----------------------------------------------------
        // SECURITY CHECK
        // Only the owner artisan can update this order.
        // -----------------------------------------------------

        if (artisanOrder.getArtisan() == null
                || artisan.getArtisanId() == null
                || artisanOrder.getArtisan().getArtisanId() == null
                || !artisanOrder.getArtisan()
                        .getArtisanId()
                        .equals(artisan.getArtisanId())) {

            return "redirect:/artisan/orders";
        }

        // -----------------------------------------------------
        // ONLY ALLOW OUR THREE STATUSES
        // NEW -> PROCESSING -> DELIVERED
        // -----------------------------------------------------

        if (!"NEW".equals(status)
                && !"PROCESSING".equals(status)
                && !"DELIVERED".equals(status)) {

            return "redirect:/artisan/orders";
        }

        // -----------------------------------------------------
        // SAVE NEW STATUS
        // -----------------------------------------------------

        artisanOrder.setOrderStatus(status);

        artisanOrderRepository.save(artisanOrder);

        // -----------------------------------------------------
        // EMAIL CUSTOMER
        // -----------------------------------------------------

        String artisanName = "Artisan";

        if (artisan.getUser() != null
                && artisan.getUser().getFullName() != null) {

            artisanName =
                    artisan.getUser().getFullName();
        }

        orderStatusEmailService.sendStatusUpdateEmail(
                artisanOrder.getOrder(),
                artisanName,
                status
        );

        return "redirect:/artisan/orders?status=updated";
    }

    // =========================================================
    // VIEW MODEL
    // =========================================================

    public static class ArtisanOrderView {

        private final ArtisanOrder artisanOrder;
        private final List<OrderItem> items;

        public ArtisanOrderView(
                ArtisanOrder artisanOrder,
                List<OrderItem> items) {

            this.artisanOrder = artisanOrder;
            this.items = items;
        }

        public ArtisanOrder getArtisanOrder() {
            return artisanOrder;
        }

        public List<OrderItem> getItems() {
            return items;
        }
    }
}
