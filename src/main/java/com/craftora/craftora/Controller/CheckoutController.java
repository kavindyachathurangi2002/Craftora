package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Customer;
import com.craftora.craftora.Entity.Order;
import com.craftora.craftora.Entity.OrderItem;
import com.craftora.craftora.Entity.Product;
import com.craftora.craftora.Repository.CustomerRepository;
import com.craftora.craftora.Repository.OrderItemRepository;
import com.craftora.craftora.Repository.OrderRepository;
import com.craftora.craftora.Repository.ProductRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CheckoutController {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public CheckoutController(
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository) {

        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    // =========================================================
    // CHECKOUT PAGE
    // =========================================================

    @GetMapping("/checkout")
    public String checkout(
            HttpSession session,
            Model model,
            @RequestParam(required = false) String success) {

        if (!canPurchase(session)) {
            return "redirect:/marketplace?auth=login";
        }

        Object userIdObject =
                session.getAttribute("loggedInUserId");

        if (!(userIdObject instanceof Long)) {
            return "redirect:/marketplace?auth=login";
        }

        Long userId = (Long) userIdObject;

        Customer customer = getOrCreateCustomer(userId);

        if (customer == null) {
            return "redirect:/customer/profile";
        }

        List<CheckoutItem> checkoutItems =
                new ArrayList<>();

        // -----------------------------------------------------
        // BUY NOW
        // -----------------------------------------------------

        Object buyNowObject =
                session.getAttribute("buyNowProductId");

        if (buyNowObject instanceof Long) {

            Long productId =
                    (Long) buyNowObject;

            Product product =
                    productRepository
                            .findById(productId)
                            .orElse(null);

            if (product != null
                    && product.getStockQuantity() != null
                    && product.getStockQuantity() > 0) {

                checkoutItems.add(
                        new CheckoutItem(product, 1)
                );
            }
        }

        // -----------------------------------------------------
        // NORMAL CART CHECKOUT
        // -----------------------------------------------------

        if (checkoutItems.isEmpty()) {

            Map<Long, Integer> cartItems =
                    getCartItems(session);

            for (Map.Entry<Long, Integer> entry
                    : cartItems.entrySet()) {

                Product product =
                        productRepository
                                .findById(entry.getKey())
                                .orElse(null);

                if (product == null) {
                    continue;
                }

                if (product.getStockQuantity() == null
                        || product.getStockQuantity() <= 0) {
                    continue;
                }

                int quantity =
                        entry.getValue();

                if (quantity > product.getStockQuantity()) {
                    quantity =
                            product.getStockQuantity();
                }

                if (quantity > 0) {

                    checkoutItems.add(
                            new CheckoutItem(
                                    product,
                                    quantity
                            )
                    );
                }
            }
        }

        if (checkoutItems.isEmpty()) {
            return "redirect:/cart";
        }

        BigDecimal total =
                BigDecimal.ZERO;

        for (CheckoutItem item : checkoutItems) {
            total =
                    total.add(
                            item.getSubtotal()
                    );
        }

        model.addAttribute("customer", customer);
        model.addAttribute("checkoutItems", checkoutItems);
        model.addAttribute("checkoutTotal", total);

        if ("true".equalsIgnoreCase(success)) {
            model.addAttribute(
                    "successMessage",
                    "Your order has been created successfully."
            );
        }

        return "checkout";
    }

    // =========================================================
    // PLACE ORDER
    // =========================================================

    @PostMapping("/checkout/place-order")
    public String placeOrder(
            HttpSession session,
            @RequestParam String deliveryName,
            @RequestParam String deliveryPhone,
            @RequestParam String deliveryCity,
            @RequestParam String deliveryAddress) {

        if (!canPurchase(session)) {
            return "redirect:/marketplace?auth=login";
        }

        Object userIdObject =
                session.getAttribute("loggedInUserId");

        if (!(userIdObject instanceof Long)) {
            return "redirect:/marketplace?auth=login";
        }

        Long userId =
                (Long) userIdObject;

        Customer customer =
                getOrCreateCustomer(userId);

        if (customer == null) {
            return "redirect:/customer/profile";
        }

        deliveryName =
                deliveryName == null
                        ? ""
                        : deliveryName.trim();

        deliveryPhone =
                deliveryPhone == null
                        ? ""
                        : deliveryPhone.trim();

        deliveryCity =
                deliveryCity == null
                        ? ""
                        : deliveryCity.trim();

        deliveryAddress =
                deliveryAddress == null
                        ? ""
                        : deliveryAddress.trim();

        if (deliveryName.isEmpty()
                || deliveryPhone.isEmpty()
                || deliveryCity.isEmpty()
                || deliveryAddress.isEmpty()) {

            return "redirect:/checkout?error=delivery";
        }

        List<CheckoutItem> checkoutItems =
                new ArrayList<>();

        Object buyNowObject =
                session.getAttribute("buyNowProductId");

        if (buyNowObject instanceof Long) {

            Long productId =
                    (Long) buyNowObject;

            Product product =
                    productRepository
                            .findById(productId)
                            .orElse(null);

            if (product != null
                    && product.getStockQuantity() != null
                    && product.getStockQuantity() > 0) {

                checkoutItems.add(
                        new CheckoutItem(
                                product,
                                1
                        )
                );
            }

        } else {

            Map<Long, Integer> cartItems =
                    getCartItems(session);

            for (Map.Entry<Long, Integer> entry
                    : cartItems.entrySet()) {

                Product product =
                        productRepository
                                .findById(entry.getKey())
                                .orElse(null);

                if (product == null) {
                    continue;
                }

                if (product.getStockQuantity() == null
                        || product.getStockQuantity() <= 0) {
                    continue;
                }

                int quantity =
                        entry.getValue();

                if (quantity > product.getStockQuantity()) {
                    quantity =
                            product.getStockQuantity();
                }

                if (quantity > 0) {

                    checkoutItems.add(
                            new CheckoutItem(
                                    product,
                                    quantity
                            )
                    );
                }
            }
        }

        if (checkoutItems.isEmpty()) {
            return "redirect:/cart";
        }

        BigDecimal total =
                BigDecimal.ZERO;

        for (CheckoutItem item : checkoutItems) {
            total =
                    total.add(
                            item.getSubtotal()
                    );
        }

        Order order =
                new Order();

        order.setCustomer(customer);

        order.setDeliveryName(deliveryName);
        order.setDeliveryPhone(deliveryPhone);
        order.setDeliveryCity(deliveryCity);
        order.setDeliveryAddress(deliveryAddress);
        order.setTotalAmount(total);

        order.setPaymentStatus("PENDING");
        order.setOrderStatus("PENDING");

        order =
                orderRepository.save(order);

        for (CheckoutItem checkoutItem
                : checkoutItems) {

            Product product =
                    checkoutItem.getProduct();

            OrderItem orderItem =
                    new OrderItem();

            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setArtisan(product.getArtisan());
            orderItem.setProductName(product.getProductName());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setQuantity(checkoutItem.getQuantity());
            orderItem.setSubtotal(checkoutItem.getSubtotal());

            orderItemRepository.save(orderItem);
        }

        if (buyNowObject instanceof Long) {
            session.removeAttribute("buyNowProductId");
        }

        // Do not clear normal cart until payment succeeds.
        return "redirect:/order-payment/payhere?order_id="
                + order.getOrderId();
    }

    // =========================================================
    // GET OR CREATE CUSTOMER RECORD
    // =========================================================
    /*
     * An ARTISAN is allowed to purchase marketplace products.
     * The orders table uses customer_id, so an artisan who does
     * not yet have a Customer row receives a Customer record
     * linked to the same User. This does NOT add the CUSTOMER role.
     */

    private Customer getOrCreateCustomer(Long userId) {

        Customer existing =
                customerRepository
                        .findByUserUserId(userId)
                        .orElse(null);

        if (existing != null) {
            return existing;
        }

        com.craftora.craftora.Entity.User user =
                new com.craftora.craftora.Entity.User();
        user.setUserId(userId);

        Customer customer =
                new Customer();

        customer.setUser(user);
        customer.setCreatedAt(LocalDateTime.now());

        return customerRepository.save(customer);
    }

    // =========================================================
    // CART SESSION
    // =========================================================

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getCartItems(
            HttpSession session) {

        Object cartObject =
                session.getAttribute("cartItems");

        if (cartObject instanceof Map<?, ?>) {
            return (Map<Long, Integer>) cartObject;
        }

        return new HashMap<>();
    }

    // =========================================================
    // CUSTOMER OR ARTISAN CHECK
    // =========================================================

    private boolean canPurchase(
            HttpSession session) {

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

        for (Object role :
                (List<?>) rolesObject) {

            if (role instanceof String) {

                String roleName =
                        ((String) role).trim();

                if ("CUSTOMER".equalsIgnoreCase(roleName)
                        || "ARTISAN".equalsIgnoreCase(roleName)) {

                    return true;
                }
            }
        }

        return false;
    }

    // =========================================================
    // CHECKOUT ITEM
    // =========================================================

    public static class CheckoutItem {

        private final Product product;
        private final int quantity;

        public CheckoutItem(
                Product product,
                int quantity) {

            this.product = product;
            this.quantity = quantity;
        }

        public Product getProduct() {
            return product;
        }

        public int getQuantity() {
            return quantity;
        }

        public BigDecimal getSubtotal() {

            return product.getPrice()
                    .multiply(
                            BigDecimal.valueOf(quantity)
                    );
        }
    }
}
