package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Entity.Customer;
import com.craftora.craftora.Entity.Order;
import com.craftora.craftora.Entity.OrderItem;
import com.craftora.craftora.Repository.ArtisanRepository;
import com.craftora.craftora.Repository.CustomerRepository;
import com.craftora.craftora.Repository.OrderItemRepository;
import com.craftora.craftora.Repository.OrderRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ArtisanMyOrderController {

    private final ArtisanRepository artisanRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public ArtisanMyOrderController(
            ArtisanRepository artisanRepository,
            CustomerRepository customerRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository) {
        this.artisanRepository = artisanRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @GetMapping("/artisan/my-orders")
    public String artisanMyOrders(
            HttpSession session,
            Model model) {

        Object userIdObject = session.getAttribute("loggedInUserId");
        if (!(userIdObject instanceof Long)) {
            return "redirect:/login";
        }

        Long userId = (Long) userIdObject;

        Object selectedRole = session.getAttribute("selectedRole");
        if (selectedRole == null
                || !"ARTISAN".equalsIgnoreCase(selectedRole.toString())) {
            return "redirect:/login";
        }

        Artisan artisan = artisanRepository
                .findByUserUserId(userId)
                .orElse(null);

        if (artisan == null) {
            return "redirect:/login";
        }

        Customer customer = customerRepository
                .findByUserUserId(userId)
                .orElse(null);

        List<OrderView> orderViews = new ArrayList<>();

        if (customer != null) {
            List<Order> orders =
                    orderRepository.findByCustomerOrderByOrderDateDesc(customer);

            for (Order order : orders) {
                List<OrderItem> items = orderItemRepository.findByOrder(order);
                orderViews.add(new OrderView(order, items));
            }
        }

        model.addAttribute("artisan", artisan);
        model.addAttribute("orderViews", orderViews);

        return "artisan-my-orders";
    }

    public static class OrderView {

        private final Order order;
        private final List<OrderItem> items;

        public OrderView(Order order, List<OrderItem> items) {
            this.order = order;
            this.items = items;
        }

        public Order getOrder() {
            return order;
        }

        public List<OrderItem> getItems() {
            return items;
        }
    }
}
