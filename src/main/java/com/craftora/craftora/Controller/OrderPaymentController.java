package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Entity.ArtisanOrder;
import com.craftora.craftora.Entity.Order;
import com.craftora.craftora.Entity.OrderItem;
import com.craftora.craftora.Entity.OrderPayment;
import com.craftora.craftora.Entity.Product;
import com.craftora.craftora.Repository.ArtisanOrderRepository;
import com.craftora.craftora.Repository.CartItemRepository;
import com.craftora.craftora.Repository.OrderItemRepository;
import com.craftora.craftora.Repository.OrderPaymentRepository;
import com.craftora.craftora.Repository.OrderRepository;
import com.craftora.craftora.Repository.ProductRepository;
import com.craftora.craftora.Service.OrderPayHereService;
import com.craftora.craftora.Service.SystemSettingService;
import com.craftora.craftora.Service.EmailService;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class OrderPaymentController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderPaymentRepository orderPaymentRepository;
    private final ProductRepository productRepository;
    private final ArtisanOrderRepository artisanOrderRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderPayHereService orderPayHereService;
    private final SystemSettingService systemSettingService;
    private final EmailService emailService;

    public OrderPaymentController(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderPaymentRepository orderPaymentRepository,
            ProductRepository productRepository,
            ArtisanOrderRepository artisanOrderRepository,
            CartItemRepository cartItemRepository,
            OrderPayHereService orderPayHereService,
            SystemSettingService systemSettingService,
            EmailService emailService) {

        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderPaymentRepository = orderPaymentRepository;
        this.productRepository = productRepository;
        this.artisanOrderRepository = artisanOrderRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderPayHereService = orderPayHereService;
        this.systemSettingService = systemSettingService;
        this.emailService = emailService;
    }

    // =========================================================
    // START CUSTOMER / ARTISAN PRODUCT PAYMENT
    // =========================================================

    @GetMapping("/order-payment/payhere")
    public String startPayment(
            @RequestParam("order_id") Long orderId,
            HttpSession session,
            Model model) {

        if (!canPurchase(session)) {
            return "redirect:/marketplace?auth=login";
        }

        Object userIdObject =
                session.getAttribute("loggedInUserId");

        if (!(userIdObject instanceof Long)) {
            return "redirect:/login";
        }

        Long userId = (Long) userIdObject;

        Order order =
                orderRepository.findById(orderId)
                        .orElse(null);

        if (order == null) {
            return "redirect:/cart";
        }

        if (order.getCustomer() == null
                || order.getCustomer().getUser() == null
                || !order.getCustomer()
                        .getUser()
                        .getUserId()
                        .equals(userId)) {

            return dashboardRedirect(session);
        }

        if ("PAID".equalsIgnoreCase(
                order.getPaymentStatus())) {

            return dashboardRedirect(
                    session
            ) + "?payment=already-paid";
        }

        String payHereOrderId =
                "CRAFTORA-ORDER-"
                        + orderId
                        + "-"
                        + System.currentTimeMillis();

        OrderPayment payment =
                new OrderPayment();

        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setAmount(order.getTotalAmount());
        payment.setPaymentMethod("PAYHERE");
        payment.setTransactionId(payHereOrderId);
        payment.setPaymentStatus("PENDING");
        payment.setCreatedAt(LocalDateTime.now());

        orderPaymentRepository.save(payment);

        String fullName =
                order.getCustomer()
                        .getUser()
                        .getFullName();

        String firstName = fullName;
        String lastName = "";

        if (fullName != null
                && fullName.contains(" ")) {

            String[] nameParts =
                    fullName.trim()
                            .split("\\s+", 2);

            firstName = nameParts[0];
            lastName = nameParts[1];
        }

        String email =
                order.getCustomer()
                        .getUser()
                        .getEmail();

        List<OrderItem> orderItems =
                orderItemRepository.findByOrder(order);

        StringBuilder itemNames =
                new StringBuilder();

        for (OrderItem item : orderItems) {

            if (itemNames.length() > 0) {
                itemNames.append(", ");
            }

            itemNames.append(
                    item.getProductName()
            );
        }

        Map<String, String> checkoutData =
                orderPayHereService.createCheckoutData(
                        payHereOrderId,
                        firstName,
                        lastName,
                        email,
                        order.getDeliveryPhone(),
                        order.getDeliveryAddress(),
                        order.getDeliveryCity(),
                        itemNames.toString(),
                        order.getTotalAmount()
                );

        model.addAttribute(
                "checkoutUrl",
                orderPayHereService.getCheckoutUrl()
        );

        model.addAttribute(
                "checkoutData",
                checkoutData
        );

        return "order-payhere-redirect";
    }

    // =========================================================
    // PAYMENT SUCCESS
    // =========================================================

    @Transactional
    @GetMapping("/order-payment/success")
    public synchronized String paymentSuccess(
            @RequestParam("order_id") String payHereOrderId,
            HttpSession session,
            Model model) {

        if (payHereOrderId != null
                && payHereOrderId.contains(",")) {

            payHereOrderId =
                    payHereOrderId
                            .split(",")[0]
                            .trim();
        }

        OrderPayment payment =
                orderPaymentRepository
                        .findByTransactionId(
                                payHereOrderId
                        )
                        .orElse(null);

        if (payment == null) {
            return dashboardRedirect(
                    session
            ) + "?payment=failed";
        }

        Order order =
                orderRepository
                        .findById(
                                payment.getOrderId()
                        )
                        .orElse(null);

        if (order == null) {
            return dashboardRedirect(
                    session
            ) + "?payment=failed";
        }

        if ("PAID".equalsIgnoreCase(
                payment.getPaymentStatus())) {

            model.addAttribute(
                    "dashboardUrl",
                    dashboardPath(session)
            );

            return "order-payment-success";
        }

        List<OrderItem> orderItems =
                orderItemRepository.findByOrder(order);

                // Pass all purchased products to the payment success page
// so the customer can rate each purchased product.
model.addAttribute("purchasedProducts", orderItems);

        // -----------------------------------------------------
        // UPDATE PRODUCT STOCK
        // -----------------------------------------------------

        for (OrderItem orderItem :
                orderItems) {

            Product product =
                    orderItem.getProduct();

            if (product == null) {
                continue;
            }

            Integer currentStock =
                    product.getStockQuantity();

            if (currentStock == null) {
                currentStock = 0;
            }

            int quantityPurchased =
                    orderItem.getQuantity();

            int newStock =
                    currentStock
                            - quantityPurchased;

            if (newStock < 0) {
                newStock = 0;
            }

            product.setStockQuantity(
                    newStock
            );


            // -----------------------------------------------------
// LOW STOCK EMAIL ALERT
// Send email only when stock crosses from above 5
// to 5 or below because of a customer purchase.
// -----------------------------------------------------

if (currentStock > 5
        && newStock <= 5
        && !product.isLowStockAlertSent()) {


                System.out.println("===== LOW STOCK DEBUG =====");
System.out.println("Product: " + product.getProductName());
System.out.println("Old Stock: " + currentStock);
System.out.println("Purchased Qty: " + quantityPurchased);
System.out.println("New Stock: " + newStock);
System.out.println("Alert Already Sent: " + product.isLowStockAlertSent());
System.out.println("===========================");

    if (product.getArtisan() != null
            && product.getArtisan().getUser() != null) {

        String artisanEmail =
                product.getArtisan()
                        .getUser()
                        .getEmail();

        String artisanName =
                product.getArtisan()
                        .getUser()
                        .getFullName();

        emailService.sendLowStockAlertEmail(
                artisanEmail,
                artisanName,
                product.getProductName(),
                newStock
        );

        product.setLowStockAlertSent(true);
    }
}


            productRepository.save(product);
        }

        // -----------------------------------------------------
        // CREATE ONE ARTISAN ORDER PER ARTISAN
        // -----------------------------------------------------

        Map<Long, ArtisanOrder> artisanOrderMap =
                new HashMap<>();

        for (OrderItem orderItem :
                orderItems) {

            Artisan artisan =
                    orderItem.getArtisan();

            if (artisan == null) {
                continue;
            }

            Long artisanId =
                    artisan.getArtisanId();

            ArtisanOrder artisanOrder =
                    artisanOrderMap.get(artisanId);

            if (artisanOrder == null) {

                artisanOrder =
                        new ArtisanOrder();

                artisanOrder.setOrder(order);
                artisanOrder.setArtisan(artisan);

                artisanOrder.setSubtotal(
                        BigDecimal.ZERO
                );

                artisanOrder.setCommissionAmount(
                        BigDecimal.ZERO
                );

                artisanOrder.setArtisanAmount(
                        BigDecimal.ZERO
                );

                artisanOrder.setOrderStatus(
                        "NEW"
                );

                artisanOrderMap.put(
                        artisanId,
                        artisanOrder
                );
            }

            BigDecimal itemSubtotal =
                    orderItem.getSubtotal();

            if (itemSubtotal == null) {
                itemSubtotal =
                        BigDecimal.ZERO;
            }

            BigDecimal newSubtotal =
                    artisanOrder
                            .getSubtotal()
                            .add(itemSubtotal);

            artisanOrder.setSubtotal(
                    newSubtotal
            );

            // -------------------------------------------------
            // GET PLATFORM COMMISSION FROM ADMIN SETTINGS
            // -------------------------------------------------

            BigDecimal commissionPercentage =
                    systemSettingService
                            .getPlatformCommission();

            BigDecimal commissionRate =
                    commissionPercentage
                            .movePointLeft(2);

            BigDecimal commissionAmount =
                    newSubtotal
                            .multiply(commissionRate)
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );

            BigDecimal artisanAmount =
                    newSubtotal
                            .subtract(commissionAmount)
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );

            artisanOrder.setCommissionAmount(
                    commissionAmount
            );

            artisanOrder.setArtisanAmount(
                    artisanAmount
            );
        }

        for (ArtisanOrder artisanOrder :
                artisanOrderMap.values()) {

            artisanOrderRepository.save(
                    artisanOrder
            );
        }

        payment.setPaymentStatus("PAID");

        payment.setPaidAt(
                LocalDateTime.now()
        );

        orderPaymentRepository.saveAndFlush(
                payment
        );

        order.setPaymentStatus("PAID");
        order.setOrderStatus("CONFIRMED");

        orderRepository.saveAndFlush(
                order
        );

        // -----------------------------------------------------
        // REMOVE PURCHASED PRODUCTS FROM DATABASE CART
        // -----------------------------------------------------

        if (order.getCustomer() != null
                && order.getCustomer().getUser() != null) {

            for (OrderItem orderItem : orderItems) {

                Product purchasedProduct =
                        orderItem.getProduct();

                if (purchasedProduct != null) {

                    cartItemRepository
                            .findByUserAndProduct(
                                    order.getCustomer().getUser(),
                                    purchasedProduct
                            )
                            .ifPresent(
                                    cartItemRepository::delete
                            );
                }
            }
        }

        session.removeAttribute("cartItems");
        session.removeAttribute("buyNowProductId");

        model.addAttribute(
                "dashboardUrl",
                dashboardPath(session)
        );

        return "order-payment-success";
    }

    // =========================================================
    // PAYMENT CANCEL
    // =========================================================

    @GetMapping("/order-payment/cancel")
    public String paymentCancel(
            @RequestParam(
                    value = "order_id",
                    required = false
            )
            String orderId) {

        return "redirect:/checkout?payment=cancelled";
    }

    // =========================================================
    // CUSTOMER OR ARTISAN CHECK
    // =========================================================

    private boolean canPurchase(
            HttpSession session) {

        Object userId =
                session.getAttribute(
                        "loggedInUserId"
                );

        if (userId == null) {
            return false;
        }

        Object rolesObject =
                session.getAttribute(
                        "userRoles"
                );

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
    // DASHBOARD PATH
    // =========================================================

    private String dashboardPath(
            HttpSession session) {

        Object selectedRole =
                session.getAttribute(
                        "selectedRole"
                );

        if (selectedRole instanceof String) {

            String role =
                    ((String) selectedRole).trim();

            if ("ARTISAN".equalsIgnoreCase(role)) {
                return "/artisan-dashboard";
            }

            if ("ADMIN".equalsIgnoreCase(role)) {
                return "/admin-dashboard";
            }
        }

        return "/customer-dashboard";
    }

    // =========================================================
    // DASHBOARD REDIRECT
    // =========================================================

    private String dashboardRedirect(
            HttpSession session) {

        Object selectedRole =
                session.getAttribute(
                        "selectedRole"
                );

        if (selectedRole instanceof String) {

            String role =
                    ((String) selectedRole).trim();

            if ("ARTISAN".equalsIgnoreCase(role)) {
                return "redirect:/artisan-dashboard";
            }

            if ("ADMIN".equalsIgnoreCase(role)) {
                return "redirect:/admin-dashboard";
            }
        }

        return "redirect:/customer-dashboard";
    }
}