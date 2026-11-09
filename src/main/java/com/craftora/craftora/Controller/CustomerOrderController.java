package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Customer;
import com.craftora.craftora.Entity.Order;
import com.craftora.craftora.Entity.OrderItem;
import com.craftora.craftora.Entity.OrderPayment;
import com.craftora.craftora.Repository.CustomerRepository;
import com.craftora.craftora.Repository.OrderItemRepository;
import com.craftora.craftora.Repository.OrderPaymentRepository;
import com.craftora.craftora.Repository.OrderRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class CustomerOrderController {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderPaymentRepository orderPaymentRepository;

    public CustomerOrderController(
            CustomerRepository customerRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderPaymentRepository orderPaymentRepository) {

        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderPaymentRepository = orderPaymentRepository;
    }


    // =========================================================
    // CUSTOMER ORDER HISTORY
    // =========================================================

    @GetMapping("/customer/orders")
    public String customerOrders(
            HttpSession session,
            Model model) {

        // -----------------------------------------------------
        // GET LOGGED-IN USER ID
        // -----------------------------------------------------

        Object userIdObject =
                session.getAttribute("loggedInUserId");

        if (!(userIdObject instanceof Long)) {
            return "redirect:/login";
        }

        Long userId =
                (Long) userIdObject;


        // -----------------------------------------------------
        // FIND CUSTOMER
        // -----------------------------------------------------

        Customer customer =
                customerRepository
                        .findByUserUserId(userId)
                        .orElse(null);

        if (customer == null) {
            return "redirect:/customer/profile";
        }


        // -----------------------------------------------------
        // GET CUSTOMER ORDERS
        // -----------------------------------------------------

        List<Order> orders =
                orderRepository
                        .findByCustomerOrderByOrderDateDesc(
                                customer
                        );


        // -----------------------------------------------------
        // CREATE ORDER VIEW LIST
        // -----------------------------------------------------

        List<OrderView> orderViews =
                new ArrayList<>();


        for (Order order : orders) {

            List<OrderItem> items =
                    orderItemRepository
                            .findByOrder(order);

            orderViews.add(
                    new OrderView(
                            order,
                            items
                    )
            );
        }


        // -----------------------------------------------------
        // SEND TO HTML
        // -----------------------------------------------------

        model.addAttribute(
                "orderViews",
                orderViews
        );


        return "customer-orders";
    }


    // =========================================================
    // DELETE PENDING ORDER
    // =========================================================

    @PostMapping("/customer/orders/delete")
    @Transactional
    public String deletePendingOrder(
            @RequestParam("order_id") Long orderId,
            HttpSession session) {

        // -----------------------------------------------------
        // GET LOGGED-IN USER ID
        // -----------------------------------------------------

        Object userIdObject =
                session.getAttribute("loggedInUserId");

        if (!(userIdObject instanceof Long)) {
            return "redirect:/login";
        }

        Long userId =
                (Long) userIdObject;


        // -----------------------------------------------------
        // FIND CUSTOMER
        // -----------------------------------------------------

        Customer customer =
                customerRepository
                        .findByUserUserId(userId)
                        .orElse(null);

        if (customer == null) {
            return "redirect:/customer/profile";
        }


        // -----------------------------------------------------
        // FIND ORDER
        // -----------------------------------------------------

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElse(null);

        if (order == null) {
            return "redirect:/customer/orders";
        }


        // -----------------------------------------------------
        // SECURITY CHECK
        // MAKE SURE THIS ORDER BELONGS TO THIS CUSTOMER
        // -----------------------------------------------------

        if (order.getCustomer() == null
                || order.getCustomer().getUser() == null
                || !order.getCustomer()
                        .getUser()
                        .getUserId()
                        .equals(userId)) {

            return "redirect:/customer/orders";
        }


        // -----------------------------------------------------
        // ONLY PENDING PAYMENT + PENDING ORDER CAN BE DELETED
        // -----------------------------------------------------

        boolean paymentPending =
                "PENDING".equalsIgnoreCase(
                        order.getPaymentStatus()
                );

        boolean orderPending =
                "PENDING".equalsIgnoreCase(
                        order.getOrderStatus()
                );


        if (!paymentPending || !orderPending) {

            return "redirect:/customer/orders";
        }


        // -----------------------------------------------------
        // DELETE RELATED ORDER PAYMENTS
        // -----------------------------------------------------

        List<OrderPayment> payments =
                orderPaymentRepository
                        .findByOrderId(orderId);

        if (payments != null
                && !payments.isEmpty()) {

            orderPaymentRepository.deleteAll(
                    payments
            );

            orderPaymentRepository.flush();
        }


        // -----------------------------------------------------
        // DELETE ORDER ITEMS
        // -----------------------------------------------------

        List<OrderItem> orderItems =
                orderItemRepository
                        .findByOrder(order);

        if (orderItems != null
                && !orderItems.isEmpty()) {

            orderItemRepository.deleteAll(
                    orderItems
            );

            orderItemRepository.flush();
        }


        // -----------------------------------------------------
        // DELETE ORDER
        // -----------------------------------------------------

        orderRepository.delete(order);

        orderRepository.flush();


        // -----------------------------------------------------
        // RETURN TO CUSTOMER ORDERS
        // -----------------------------------------------------

        return "redirect:/customer/orders?deleted=true";
    }


    // =========================================================
    // ORDER VIEW
    // =========================================================

    public static class OrderView {

        private final Order order;

        private final List<OrderItem> items;


        public OrderView(
                Order order,
                List<OrderItem> items) {

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