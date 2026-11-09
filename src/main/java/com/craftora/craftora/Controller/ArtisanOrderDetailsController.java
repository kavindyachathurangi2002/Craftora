package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Entity.ArtisanOrder;
import com.craftora.craftora.Entity.Customer;
import com.craftora.craftora.Entity.Order;
import com.craftora.craftora.Entity.OrderItem;
import com.craftora.craftora.Repository.ArtisanOrderRepository;
import com.craftora.craftora.Repository.ArtisanRepository;
import com.craftora.craftora.Repository.CustomerRepository;
import com.craftora.craftora.Repository.OrderItemRepository;
import com.craftora.craftora.Repository.OrderRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ArtisanOrderDetailsController {

    private final ArtisanRepository artisanRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ArtisanOrderRepository artisanOrderRepository;

    public ArtisanOrderDetailsController(
            ArtisanRepository artisanRepository,
            CustomerRepository customerRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ArtisanOrderRepository artisanOrderRepository) {

        this.artisanRepository = artisanRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.artisanOrderRepository = artisanOrderRepository;
    }

    @GetMapping("/artisan/my-orders/details/{orderId}")
    public String orderDetails(
            @PathVariable Long orderId,
            HttpSession session,
            Model model) {

        if (!isArtisan(session)) {
            return "redirect:/login";
        }

        Object userIdObject =
                session.getAttribute("loggedInUserId");

        if (userIdObject == null) {
            return "redirect:/login";
        }

        Long userId;

        try {
            userId = Long.valueOf(userIdObject.toString());
        } catch (Exception e) {
            return "redirect:/login";
        }

        Artisan artisan =
                artisanRepository.findByUserUserId(userId)
                        .orElse(null);

        if (artisan == null) {
            return "redirect:/artisan-dashboard";
        }

        Customer customer =
                customerRepository.findByUserUserId(userId)
                        .orElse(null);

        if (customer == null) {
            return "redirect:/artisan/my-orders";
        }

        Order order =
                orderRepository.findById(orderId)
                        .orElse(null);

        if (order == null) {
            return "redirect:/artisan/my-orders";
        }

        if (order.getCustomer() == null
                || order.getCustomer().getCustomerId() == null
                || !order.getCustomer().getCustomerId()
                        .equals(customer.getCustomerId())) {

            return "redirect:/artisan/my-orders";
        }

        List<OrderItem> orderItems =
                orderItemRepository.findByOrder(order);

        List<ArtisanOrder> artisanOrders =
                artisanOrderRepository.findByOrder(order);

        List<ArtisanOrderTrackingView> trackingViews =
                new ArrayList<>();

        for (ArtisanOrder artisanOrder : artisanOrders) {

            String artisanName = "Artisan";

            if (artisanOrder.getArtisan() != null
                    && artisanOrder.getArtisan().getUser() != null
                    && artisanOrder.getArtisan().getUser().getFullName() != null) {

                artisanName =
                        artisanOrder.getArtisan()
                                .getUser()
                                .getFullName();
            }

            List<OrderItem> artisanItems =
                    new ArrayList<>();

            for (OrderItem item : orderItems) {

                if (item.getArtisan() != null
                        && artisanOrder.getArtisan() != null
                        && item.getArtisan().getArtisanId() != null
                        && artisanOrder.getArtisan().getArtisanId() != null
                        && item.getArtisan().getArtisanId()
                                .equals(
                                        artisanOrder.getArtisan()
                                                .getArtisanId()
                                )) {

                    artisanItems.add(item);
                }
            }

            trackingViews.add(
                    new ArtisanOrderTrackingView(
                            artisanOrder,
                            artisanName,
                            artisanItems
                    )
            );
        }

        model.addAttribute("artisan", artisan);
        model.addAttribute("order", order);
        model.addAttribute("orderItems", orderItems);
        model.addAttribute("trackingViews", trackingViews);

        return "artisan-order-details";
    }

    private boolean isArtisan(HttpSession session) {

        Object userId =
                session.getAttribute("loggedInUserId");

        if (userId == null) {
            return false;
        }

        Object rolesObject =
                session.getAttribute("userRoles");

        if (!(rolesObject instanceof List<?>)) {
            return false;
        }

        for (Object role : (List<?>) rolesObject) {

            if (role instanceof String
                    && "ARTISAN".equalsIgnoreCase((String) role)) {

                return true;
            }
        }

        return false;
    }

    public static class ArtisanOrderTrackingView {

        private final ArtisanOrder artisanOrder;
        private final String artisanName;
        private final List<OrderItem> items;

        public ArtisanOrderTrackingView(
                ArtisanOrder artisanOrder,
                String artisanName,
                List<OrderItem> items) {

            this.artisanOrder = artisanOrder;
            this.artisanName = artisanName;
            this.items = items;
        }

        public ArtisanOrder getArtisanOrder() {
            return artisanOrder;
        }

        public String getArtisanName() {
            return artisanName;
        }

        public List<OrderItem> getItems() {
            return items;
        }
    }
}