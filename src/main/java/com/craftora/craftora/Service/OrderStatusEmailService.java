package com.craftora.craftora.Service;

import com.craftora.craftora.Entity.Order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class OrderStatusEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public OrderStatusEmailService(
            JavaMailSender mailSender) {

        this.mailSender = mailSender;
    }

    // =========================================================
    // SEND CUSTOMER ORDER STATUS EMAIL
    // =========================================================

    public void sendStatusUpdateEmail(
            Order order,
            String artisanName,
            String newStatus) {

        if (order == null
                || order.getCustomer() == null
                || order.getCustomer().getUser() == null) {

            return;
        }

        String customerEmail =
                order.getCustomer()
                        .getUser()
                        .getEmail();

        if (customerEmail == null
                || customerEmail.isBlank()) {

            return;
        }

        String customerName =
                order.getCustomer()
                        .getUser()
                        .getFullName();

        if (customerName == null
                || customerName.isBlank()) {

            customerName = "Customer";
        }

        String subject =
                "Craftora Order #" +
                order.getOrderId() +
                " Status Update";

        String statusMessage;

        if ("PROCESSING".equalsIgnoreCase(newStatus)) {

            statusMessage =
                    "Your artisan has started processing your order.";

        } else if ("DELIVERED".equalsIgnoreCase(newStatus)) {

            statusMessage =
                    "Your artisan has marked your order as delivered.";

        } else {

            statusMessage =
                    "Your order status has been updated.";
        }

        StringBuilder body =
                new StringBuilder();

        body.append("Hello ")
                .append(customerName)
                .append(",\n\n");

        body.append(
                "There is an update to your Craftora order.\n\n"
        );

        body.append("Order Number: #")
                .append(order.getOrderId())
                .append("\n");

        body.append("Artisan: ")
                .append(artisanName)
                .append("\n");

        body.append("Current Status: ")
                .append(newStatus)
                .append("\n\n");

        body.append(statusMessage)
                .append("\n\n");

        body.append(
                "You can log in to your Craftora account "
                + "to view the full order tracking details.\n\n"
        );

        body.append("Thank you for using Craftora.\n")
                .append("Craftora Team");

        try {

            SimpleMailMessage message =
                    new SimpleMailMessage();

            if (fromEmail != null
                    && !fromEmail.isBlank()) {

                message.setFrom(fromEmail);
            }

            message.setTo(customerEmail);
            message.setSubject(subject);
            message.setText(body.toString());

            mailSender.send(message);

        } catch (Exception e) {

            /*
             * Do not fail the order-status update if the
             * email provider is temporarily unavailable.
             */
            System.err.println(
                    "Craftora order status email could not be sent: "
                            + e.getMessage()
            );
        }
    }
}
