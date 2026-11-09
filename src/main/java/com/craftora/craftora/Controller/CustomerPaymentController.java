package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Customer;
import com.craftora.craftora.Entity.Event;
import com.craftora.craftora.Entity.EventPayment;
import com.craftora.craftora.Entity.OrderPayment;

import com.craftora.craftora.Repository.CustomerRepository;
import com.craftora.craftora.Repository.OrderPaymentRepository;
import com.craftora.craftora.Repository.EventPaymentRepository;
import com.craftora.craftora.Repository.EventRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class CustomerPaymentController {

    private final CustomerRepository customerRepository;
    private final OrderPaymentRepository orderPaymentRepository;
    private final EventPaymentRepository eventPaymentRepository;
    private final EventRepository eventRepository;

    public CustomerPaymentController(
            CustomerRepository customerRepository,
            OrderPaymentRepository orderPaymentRepository,
            EventPaymentRepository eventPaymentRepository,
            EventRepository eventRepository) {

        this.customerRepository = customerRepository;
        this.orderPaymentRepository = orderPaymentRepository;
        this.eventPaymentRepository = eventPaymentRepository;
        this.eventRepository = eventRepository;
    }

    @GetMapping("/customer/payments")
    public String customerPayments(
            HttpSession session,
            Model model) {

        Object userIdObject =
                session.getAttribute("loggedInUserId");

        if (userIdObject == null) {
            return "redirect:/login";
        }

        Long userId =
                Long.valueOf(userIdObject.toString());

        Customer customer =
                customerRepository
                        .findByUserUserId(userId)
                        .orElse(null);

        if (customer == null) {
            return "redirect:/login";
        }

        // Product order payments
        List<OrderPayment> payments =
                orderPaymentRepository.findByUserId(userId);

        payments.sort(
                Comparator.comparing(
                        OrderPayment::getCreatedAt,
                        Comparator.nullsLast(
                                Comparator.naturalOrder()
                        )
                ).reversed()
        );

        // Event registration payments
        List<EventPayment> eventPayments =
                eventPaymentRepository.findByUserId(userId);

        eventPayments.sort(
                Comparator.comparing(
                        EventPayment::getCreatedAt,
                        Comparator.nullsLast(
                                Comparator.naturalOrder()
                        )
                ).reversed()
        );

        // Event names for the event payment table
        Map<Long, String> eventNames =
                eventPayments.stream()
                        .map(EventPayment::getEventId)
                        .distinct()
                        .map(eventId ->
                                eventRepository.findById(eventId)
                                        .orElse(null)
                        )
                        .filter(event -> event != null)
                        .collect(Collectors.toMap(
                                Event::getEventId,
                                Event::getEventTitle
                        ));

        // Successful product payments
        long paidOrderCount =
                payments.stream()
                        .filter(p -> "PAID".equalsIgnoreCase(
                                p.getPaymentStatus()))
                        .count();

        // Successful event payments
        long paidEventCount =
                eventPayments.stream()
                        .filter(p -> "PAID".equalsIgnoreCase(
                                p.getPaymentStatus()))
                        .count();

        long totalPayments =
                paidOrderCount + paidEventCount;

        // Total amount paid for products
        BigDecimal orderAmount =
                payments.stream()
                        .filter(p -> "PAID".equalsIgnoreCase(
                                p.getPaymentStatus()))
                        .map(OrderPayment::getAmount)
                        .filter(amount -> amount != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total amount paid for events
        BigDecimal eventAmount =
                eventPayments.stream()
                        .filter(p -> "PAID".equalsIgnoreCase(
                                p.getPaymentStatus()))
                        .map(EventPayment::getAmount)
                        .filter(amount -> amount != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmountPaid =
                orderAmount.add(eventAmount);

        model.addAttribute("customer", customer);

        // Existing product payments
        model.addAttribute("payments", payments);

        // New event payments
        model.addAttribute("eventPayments", eventPayments);
        model.addAttribute("eventNames", eventNames);

        model.addAttribute("totalPayments", totalPayments);
        model.addAttribute("totalAmountPaid", totalAmountPaid);

        model.addAttribute(
                "totalTransactions",
                payments.size() + eventPayments.size()
        );

        return "customer-payments";
    }
}