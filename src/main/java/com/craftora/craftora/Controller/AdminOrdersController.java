package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Entity.ArtisanOrder;
import com.craftora.craftora.Entity.ArtisanPayment;
import com.craftora.craftora.Entity.Order;
import com.craftora.craftora.Entity.OrderItem;
import com.craftora.craftora.Entity.User;
import com.craftora.craftora.Repository.ArtisanOrderRepository;
import com.craftora.craftora.Repository.ArtisanRepository;
import com.craftora.craftora.Repository.ArtisanPaymentRepository;
import com.craftora.craftora.Repository.OrderItemRepository;
import com.craftora.craftora.Repository.OrderRepository;
import com.craftora.craftora.Repository.UserRepository;
import com.craftora.craftora.Service.SystemSettingService;
import com.craftora.craftora.Entity.EventPayment;
import com.craftora.craftora.Repository.EventPaymentRepository;


import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
public class AdminOrdersController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ArtisanOrderRepository artisanOrderRepository;
    private final ArtisanRepository artisanRepository;
    private final ArtisanPaymentRepository artisanPaymentRepository;
    private final UserRepository userRepository;
    private final SystemSettingService systemSettingService;
    private final EventPaymentRepository eventPaymentRepository;
    
    

    public AdminOrdersController(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ArtisanOrderRepository artisanOrderRepository,
            ArtisanRepository artisanRepository,
            ArtisanPaymentRepository artisanPaymentRepository,
            UserRepository userRepository,
            EventPaymentRepository eventPaymentRepository,
            SystemSettingService systemSettingService) {

        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.artisanOrderRepository = artisanOrderRepository;
        this.artisanRepository = artisanRepository;
        this.artisanPaymentRepository = artisanPaymentRepository;
        this.userRepository = userRepository;
        this.systemSettingService = systemSettingService;
        this.eventPaymentRepository = eventPaymentRepository;
    }

    @GetMapping("/admin/orders")
    public String adminOrders(
            @RequestParam(value = "period", defaultValue = "all")
            String period,

            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            HttpSession session,
            Model model) {

        User admin = getLoggedInUser(session);

        if (admin == null || !isAdmin(admin)) {
            return "redirect:/login";
        }

        Artisan adminArtisan =
                artisanRepository
                        .findByUserUserId(admin.getUserId())
                        .orElse(null);

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        String periodLabel = "All Orders";

        if ("lastMonth".equalsIgnoreCase(period)) {
            LocalDate firstDayThisMonth =
                    LocalDate.now().withDayOfMonth(1);

            LocalDate firstDayLastMonth =
                    firstDayThisMonth.minusMonths(1);

            startDate = firstDayLastMonth.atStartOfDay();
            endDate = firstDayThisMonth.atStartOfDay();
            periodLabel = "Last Month";

        } else if ("last3Months".equalsIgnoreCase(period)) {
            startDate = now.minusMonths(3);
            endDate = now;
            periodLabel = "Last 3 Months";

        } else if ("year".equalsIgnoreCase(period)) {
            LocalDate firstDayOfYear =
                    LocalDate.now().withDayOfYear(1);

            startDate = firstDayOfYear.atStartOfDay();
            endDate = now;
            periodLabel = "This Year";

        } else if ("custom".equalsIgnoreCase(period)
                && from != null
                && to != null) {

            if (to.isBefore(from)) {
                LocalDate temp = from;
                from = to;
                to = temp;
            }

            startDate = from.atStartOfDay();
            endDate = to.atTime(LocalTime.MAX);

            periodLabel =
                    from.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                    + " - "
                    + to.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        }

        List<Order> allOrders =
                orderRepository.findAll();

        List<OrderView> orderViews =
                new ArrayList<>();

        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal totalArtisanRevenue = BigDecimal.ZERO;

        int paidOrderCount = 0;

        for (Order order : allOrders) {

            LocalDateTime orderDate = order.getOrderDate();

            if (startDate != null
                    && (orderDate == null
                    || orderDate.isBefore(startDate))) {
                continue;
            }

            if (endDate != null
                    && (orderDate == null
                    || orderDate.isAfter(endDate))) {
                continue;
            }

            List<OrderItem> items =
                    orderItemRepository.findByOrder(order);

            List<ArtisanOrder> artisanOrders =
                    artisanOrderRepository.findByOrder(order);

            BigDecimal orderCommission = BigDecimal.ZERO;
            BigDecimal orderArtisanRevenue = BigDecimal.ZERO;

            for (ArtisanOrder artisanOrder : artisanOrders) {

                if (artisanOrder.getCommissionAmount() != null) {
                    orderCommission =
                            orderCommission.add(
                                    artisanOrder.getCommissionAmount()
                            );
                }

                if (artisanOrder.getArtisanAmount() != null) {
                    orderArtisanRevenue =
                            orderArtisanRevenue.add(
                                    artisanOrder.getArtisanAmount()
                            );
                }
            }

            boolean paid =
                    "PAID".equalsIgnoreCase(
                            order.getPaymentStatus()
                    );

            BigDecimal orderSales =
                    paid && order.getTotalAmount() != null
                            ? order.getTotalAmount()
                            : BigDecimal.ZERO;

            /*
             * Some older orders were created before ArtisanOrder
             * records were created during successful payment.
             * For those orders, the artisan is still available
             * through OrderItem.artisan. Use that information as
             * a display fallback and calculate the standard 3%
             * commission for the paid order.
             */
            if (paid
                    && artisanOrders.isEmpty()
                    && orderSales.compareTo(BigDecimal.ZERO) > 0) {

                orderCommission =
                        orderSales
                                .multiply(new BigDecimal("0.03"))
                                .setScale(2, java.math.RoundingMode.HALF_UP);

                orderArtisanRevenue =
                        orderSales.subtract(orderCommission);
            }

            if (paid) {
                paidOrderCount++;
                totalSales = totalSales.add(orderSales);
            }

            totalCommission =
                    totalCommission.add(orderCommission);

            totalArtisanRevenue =
                    totalArtisanRevenue.add(orderArtisanRevenue);

            String customerName = "Customer";

            if (order.getCustomer() != null
                    && order.getCustomer().getUser() != null) {

                String fullName =
                        order.getCustomer()
                                .getUser()
                                .getFullName();

                if (fullName != null
                        && !fullName.trim().isEmpty()) {
                    customerName = fullName;
                }
            }

            List<String> artisanNames = new ArrayList<>();

            for (OrderItem item : items) {

                if (item.getArtisan() != null
                        && item.getArtisan().getUser() != null
                        && item.getArtisan().getUser().getFullName() != null) {

                    String artisanName =
                            item.getArtisan()
                                    .getUser()
                                    .getFullName();

                    if (!artisanName.trim().isEmpty()
                            && !artisanNames.contains(artisanName)) {
                        artisanNames.add(artisanName);
                    }
                }
            }

            orderViews.add(
                    new OrderView(
                            order,
                            items,
                            artisanOrders,
                            artisanNames,
                            customerName,
                            orderSales,
                            orderCommission,
                            orderArtisanRevenue
                    )
            );
        }

        BigDecimal registrationFees = BigDecimal.ZERO;

        List<ArtisanPayment> registrationPayments =
                artisanPaymentRepository.findAll();

        for (ArtisanPayment payment : registrationPayments) {

            if (payment == null
                    || !"PAID".equalsIgnoreCase(
                            payment.getPaymentStatus())
                    || payment.getAmount() == null) {
                continue;
            }

            LocalDateTime paidAt = payment.getPaidAt();

            if (startDate != null
                    && (paidAt == null || paidAt.isBefore(startDate))) {
                continue;
            }

            if (endDate != null
                    && (paidAt == null || paidAt.isAfter(endDate))) {
                continue;
            }

            registrationFees = registrationFees.add(
                    payment.getAmount()
            );
        }

       // ==========================================
// PAID EVENT REGISTRATION REVENUE
// ==========================================

BigDecimal eventRegistrationRevenue = BigDecimal.ZERO;

List<EventPayment> eventPayments =
        eventPaymentRepository.findAll();

for (EventPayment payment : eventPayments) {

    if (payment == null
            || !"PAID".equalsIgnoreCase(payment.getPaymentStatus())
            || payment.getAmount() == null) {
        continue;
    }

    LocalDateTime paidAt = payment.getPaidAt();

    if (startDate != null
            && (paidAt == null || paidAt.isBefore(startDate))) {
        continue;
    }

    if (endDate != null
            && (paidAt == null || paidAt.isAfter(endDate))) {
        continue;
    }

    eventRegistrationRevenue =
            eventRegistrationRevenue.add(payment.getAmount());
}

// ==========================================
// TOTAL CRAFTORA REVENUE
// ==========================================

BigDecimal totalCraftoraRevenue =
        registrationFees
                .add(totalCommission)
                .add(eventRegistrationRevenue);


        model.addAttribute("admin", admin);
        model.addAttribute("platformCommission",
                systemSettingService.getPlatformCommission());
        model.addAttribute("artisan", adminArtisan);
        model.addAttribute("orderViews", orderViews);
        model.addAttribute("period", period);
        model.addAttribute("periodLabel", periodLabel);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("totalSales", totalSales);
        model.addAttribute("totalCommission", totalCommission);
        model.addAttribute("registrationFees", registrationFees);
        model.addAttribute("totalCraftoraRevenue", totalCraftoraRevenue);
        model.addAttribute("eventRegistrationRevenue", eventRegistrationRevenue);
        model.addAttribute(
                "totalArtisanRevenue",
                totalArtisanRevenue
        );
        model.addAttribute("paidOrderCount", paidOrderCount);
        
        model.addAttribute("artisanPayments", registrationPayments);
        model.addAttribute("eventPaymentRecords", eventPayments);

        return "admin-orders";
    }

    private User getLoggedInUser(HttpSession session) {

        Object userIdObject =
                session.getAttribute("loggedInUserId");

        if (userIdObject != null) {
            try {
                Long userId =
                        Long.valueOf(userIdObject.toString());

                User user =
                        userRepository
                                .findById(userId)
                                .orElse(null);

                if (user != null) {
                    return user;
                }

            } catch (Exception ignored) {
            }
        }

        Object loggedInUser =
                session.getAttribute("loggedInUser");

        if (loggedInUser instanceof User) {
            User user = (User) loggedInUser;

            if (user.getUserId() != null) {
                return user;
            }
        }

        return null;
    }

    private boolean isAdmin(User user) {

        if (user == null || user.getRoles() == null) {
            return false;
        }

        return user.getRoles()
                .stream()
                .anyMatch(role ->
                        role.getRoleName()
                                .equalsIgnoreCase("ADMIN")
                );
    }

    public static class OrderView {

        private final Order order;
        private final List<OrderItem> items;
        private final List<ArtisanOrder> artisanOrders;
        private final List<String> artisanNames;
        private final String customerName;
        private final BigDecimal salesAmount;
        private final BigDecimal commissionAmount;
        private final BigDecimal artisanRevenue;

        public OrderView(
                Order order,
                List<OrderItem> items,
                List<ArtisanOrder> artisanOrders,
                List<String> artisanNames,
                String customerName,
                BigDecimal salesAmount,
                BigDecimal commissionAmount,
                BigDecimal artisanRevenue) {

            this.order = order;
            this.items = items;
            this.artisanOrders = artisanOrders;
            this.artisanNames = artisanNames;
            this.customerName = customerName;
            this.salesAmount = salesAmount;
            this.commissionAmount = commissionAmount;
            this.artisanRevenue = artisanRevenue;
        }

        public Order getOrder() {
            return order;
        }

        public List<OrderItem> getItems() {
            return items;
        }

        public List<ArtisanOrder> getArtisanOrders() {
            return artisanOrders;
        }

        public List<String> getArtisanNames() {
            return artisanNames;
        }

        public String getCustomerName() {
            return customerName;
        }

        public BigDecimal getSalesAmount() {
            return salesAmount;
        }

        public BigDecimal getCommissionAmount() {
            return commissionAmount;
        }

        /**
         * Historical commission percentage for this order.
         * It is derived from the commission amount that was already
         * stored for the order, so changing the current admin setting
         * does not change old orders.
         */
        public BigDecimal getCommissionPercentage() {

            if (salesAmount == null
                    || commissionAmount == null
                    || salesAmount.compareTo(BigDecimal.ZERO) <= 0
                    || commissionAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }

            return commissionAmount
                    .multiply(new BigDecimal("100"))
                    .divide(
                            salesAmount,
                            2,
                            java.math.RoundingMode.HALF_UP
                    );
        }

        public BigDecimal getArtisanRevenue() {
            return artisanRevenue;
        }
    }
}
