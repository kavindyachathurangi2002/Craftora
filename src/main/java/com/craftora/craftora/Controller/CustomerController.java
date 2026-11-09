package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Customer;
import com.craftora.craftora.Entity.User;
import com.craftora.craftora.Entity.Order;
import com.craftora.craftora.Entity.OrderItem;

import com.craftora.craftora.Repository.CustomerRepository;
import com.craftora.craftora.Repository.UserRepository;
import com.craftora.craftora.Repository.OrderItemRepository;
import com.craftora.craftora.Repository.OrderRepository;
import com.craftora.craftora.Repository.RatingRepository;
import com.craftora.craftora.Entity.EventRegistration;
import com.craftora.craftora.Repository.EventRegistrationRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.UUID;

@Controller
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RatingRepository ratingRepository;
    private final EventRegistrationRepository eventRegistrationRepository;

    public CustomerController(
            CustomerRepository customerRepository,
            UserRepository userRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            RatingRepository ratingRepository,
        EventRegistrationRepository eventRegistrationRepository) {

        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.ratingRepository = ratingRepository;
        this.eventRegistrationRepository = eventRegistrationRepository;
    }

    // =========================================================
    // CUSTOMER DASHBOARD
    // =========================================================

    @GetMapping("/customer-dashboard")
    public String customerDashboard(
            HttpSession session,
            Model model) {

        Object userIdObject =
                session.getAttribute("loggedInUserId");

        if (userIdObject == null) {
            return "redirect:/login";
        }

        Long userId = Long.valueOf(userIdObject.toString());

        Customer customer = customerRepository
                .findByUserUserId(userId)
                .orElse(null);

        if (customer == null) {
            return "redirect:/login";
        }

        model.addAttribute("customer", customer);

        // =====================================================
// WORKSHOP CERTIFICATE NOTIFICATIONS
// =====================================================

List<EventRegistration> availableCertificates =
        new ArrayList<>();

List<EventRegistration> customerRegistrations =
        eventRegistrationRepository
                .findByUserIdOrderByRegisteredAtDesc(userId);

for (EventRegistration registration : customerRegistrations) {

    if ("CUSTOMER".equalsIgnoreCase(
                registration.getRegistrationRole())
            && "ATTENDED".equalsIgnoreCase(
                registration.getAttendanceStatus())
            && Boolean.TRUE.equals(
                registration.getCertificateAvailable())) {

        availableCertificates.add(registration);
    }
}

model.addAttribute(
        "availableCertificates",
        availableCertificates);

        // =====================================================
// RATE YOUR PURCHASE
// Show only products from the latest PAID order
// that have not been reviewed or cancelled.
// =====================================================

List<OrderItem> purchasedProducts = new ArrayList<>();

Set<Long> reviewedProductIds = new HashSet<>();

List<Order> customerOrders =
        orderRepository.findByCustomerOrderByOrderDateDesc(customer);

for (Order order : customerOrders) {

    if (!"PAID".equalsIgnoreCase(order.getPaymentStatus())) {
        continue;
    }

    List<OrderItem> orderItems =
            orderItemRepository.findByOrder(order);

    for (OrderItem item : orderItems) {

        if (item.getProduct() == null) {
            continue;
        }

        // Skip products cancelled by the customer
        if (item.isReviewPromptDismissed()) {
            continue;
        }

        Long productId =
                item.getProduct().getProductId();

        boolean alreadyReviewed =
                ratingRepository
                        .findByCustomerCustomerIdAndProductProductId(
                                customer.getCustomerId(),
                                productId
                        )
                        .isPresent();

        // Skip products that have already been reviewed
        if (alreadyReviewed) {
            reviewedProductIds.add(productId);
            continue;
        }

        purchasedProducts.add(item);
    }

    // Only check the latest PAID order
    break;
}

model.addAttribute(
        "purchasedProducts",
        purchasedProducts
);

model.addAttribute(
        "reviewedProductIds",
        reviewedProductIds
);
        return "customer-dashboard";
    }

    // =========================================================
    // CANCEL RATE-YOUR-PURCHASE PROMPT
    // =========================================================

    @PostMapping("/customer/cancel-rating")
    public String cancelRatingPrompt(
            @RequestParam("orderItemId") Long orderItemId,
            HttpSession session) {

        Object userIdObject =
                session.getAttribute("loggedInUserId");

        if (userIdObject == null) {
            return "redirect:/login";
        }

        Long userId =
                Long.valueOf(userIdObject.toString());

        Customer customer = customerRepository
                .findByUserUserId(userId)
                .orElse(null);

        if (customer == null) {
            return "redirect:/login";
        }

        OrderItem item = orderItemRepository
                .findById(orderItemId)
                .orElse(null);

        if (item == null ||
                item.getOrder() == null ||
                item.getOrder().getCustomer() == null) {

            return "redirect:/customer-dashboard";
        }

        Order order = item.getOrder();

        // Check whether this order belongs to the logged-in customer
        if (!customer.getCustomerId().equals(
                order.getCustomer().getCustomerId())) {

            return "redirect:/customer-dashboard";
        }

        // Only paid purchases can be cancelled from this section
        if (!"PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            return "redirect:/customer-dashboard";
        }

        // Save cancellation in the database
        item.setReviewPromptDismissed(true);

        orderItemRepository.save(item);

        return "redirect:/customer-dashboard";
    }

    // =========================================================
    // CUSTOMER PROFILE
    // =========================================================

    @GetMapping("/customer/profile")
    public String customerProfile(
            HttpSession session,
            Model model) {

        Object userIdObject =
                session.getAttribute("loggedInUserId");

        if (userIdObject == null) {
            return "redirect:/login";
        }

        Long userId =
                Long.valueOf(userIdObject.toString());

        Customer customer = customerRepository
                .findByUserUserId(userId)
                .orElse(null);

        if (customer == null) {
            return "redirect:/login";
        }

        model.addAttribute("customer", customer);

        return "customer-profile";
    }

    // =========================================================
    // SAVE / UPDATE CUSTOMER PROFILE
    // =========================================================

    @PostMapping("/customer/profile/save")
    public String saveCustomerProfile(
            HttpSession session,
            @RequestParam("phone") String phone,
            @RequestParam("address") String address,
            @RequestParam("city") String city,
            @RequestParam(value = "profileImage", required = false)
                    MultipartFile profileImage,
            Model model) {

        Object userIdObject =
                session.getAttribute("loggedInUserId");

        if (userIdObject == null) {
            return "redirect:/login";
        }

        Long userId =
                Long.valueOf(userIdObject.toString());

        Customer customer = customerRepository
                .findByUserUserId(userId)
                .orElse(null);

        if (customer == null) {
            return "redirect:/login";
        }

        // =====================================================
        // UPDATE USER INFORMATION
        // =====================================================

        User user = customer.getUser();

        if (user != null) {

            user.setPhone(
                    phone != null
                            ? phone.trim()
                            : ""
            );

            userRepository.save(user);
        }

        // =====================================================
        // UPDATE CUSTOMER INFORMATION
        // =====================================================

        customer.setAddress(
                address != null
                        ? address.trim()
                        : ""
        );

        customer.setCity(
                city != null
                        ? city.trim()
                        : ""
        );

        // =====================================================
        // PROFILE IMAGE
        // =====================================================

        if (profileImage != null &&
                !profileImage.isEmpty()) {

            try {

                String uploadDirectory =
                        "uploads/customers/";

                Path uploadPath =
                        Paths.get(uploadDirectory);

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                // Delete old image
                String oldImage =
                        customer.getProfileImage();

                if (oldImage != null &&
                        !oldImage.isEmpty()) {

                    try {

                        String oldFileName =
                                oldImage.substring(
                                        oldImage.lastIndexOf("/") + 1
                                );

                        Path oldFile =
                                uploadPath.resolve(oldFileName);

                        Files.deleteIfExists(oldFile);

                    } catch (Exception ignored) {
                        // Continue profile update
                    }
                }

                // Create new filename
                String originalFilename =
                        profileImage.getOriginalFilename();

                String extension = "";

                if (originalFilename != null &&
                        originalFilename.contains(".")) {

                    extension =
                            originalFilename.substring(
                                    originalFilename.lastIndexOf(".")
                            );
                }

                String newFileName =
                        UUID.randomUUID().toString() + extension;

                // Save image
                Path filePath =
                        uploadPath.resolve(newFileName);

                Files.copy(
                        profileImage.getInputStream(),
                        filePath
                );

                // Save image path
                customer.setProfileImage(
                        "/uploads/customers/" + newFileName
                );

            } catch (IOException e) {

                model.addAttribute(
                        "error",
                        "Unable to upload profile image."
                );

                model.addAttribute(
                        "customer",
                        customer
                );

                return "customer-profile";
            }
        }

        // =====================================================
        // SAVE CUSTOMER
        // =====================================================

        customer.setCreatedAt(
                customer.getCreatedAt() != null
                        ? customer.getCreatedAt()
                        : LocalDateTime.now()
        );

        customerRepository.save(customer);

        return "redirect:/customer/profile?success=true";
    }
}