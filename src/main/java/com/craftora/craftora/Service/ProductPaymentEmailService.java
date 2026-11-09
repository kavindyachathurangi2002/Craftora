package com.craftora.craftora.Service;

import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ProductPaymentEmailService {

    @Autowired
    private JavaMailSender mailSender;

    // =====================================================
    // SEND PRODUCT PAYMENT RECEIPT EMAIL
    // =====================================================

    public void sendOrderPaymentReceipt(
            String toEmail,
            String customerName,
            Long orderId,
            BigDecimal amount,
            LocalDateTime paidAt,
            byte[] pdfReceipt) throws Exception {

        // -------------------------------------------------
        // CREATE EMAIL
        // -------------------------------------------------

        MimeMessage message =
                mailSender.createMimeMessage();

        MimeMessageHelper helper =
                new MimeMessageHelper(
                        message,
                        true
                );

        // -------------------------------------------------
        // EMAIL DETAILS
        // -------------------------------------------------

        helper.setTo(toEmail);

        helper.setSubject(
                "Craftora - Product Order Payment Receipt"
        );

        // -------------------------------------------------
        // PAYMENT DATE
        // -------------------------------------------------

        String paymentDate = "-";

        if (paidAt != null) {
            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(
                            "dd-MM-yyyy HH:mm:ss"
                    );

            paymentDate = paidAt.format(formatter);
        }

        // -------------------------------------------------
        // EMAIL BODY
        // -------------------------------------------------

        String emailBody =
                "Dear " + safeValue(customerName) + ",\n\n"

                + "Your Craftora product order payment has been completed successfully.\n\n"

                + "Order ID: "
                + (orderId != null ? orderId : "-")
                + "\n"

                + "Payment Amount: LKR "
                + formatAmount(amount)
                + "\n"

                + "Payment Method: PayHere\n"

                + "Payment Status: PAID\n"

                + "Payment Date: "
                + paymentDate
                + "\n\n"

                + "Delivery: FREE DELIVERY\n\n"

                + "Your order will be delivered directly by the relevant artisan. "
                + "If your order contains products from multiple artisans, "
                + "you may receive separate deliveries.\n\n"

                + "Expected delivery: within 2 days.\n\n"

                + "Please find your product payment receipt attached "
                + "to this email as a PDF.\n\n"

                + "Thank you for shopping with Craftora.\n\n"

                + "Best Regards,\n"
                + "Craftora Team";

        helper.setText(emailBody);

        // -------------------------------------------------
        // ATTACH PRODUCT PAYMENT PDF
        // -------------------------------------------------

        helper.addAttachment(
                "Craftora-Product-Payment-Receipt-Order-"
                        + (orderId != null ? orderId : "Unknown")
                        + ".pdf",
                new ByteArrayResource(pdfReceipt)
        );

        // -------------------------------------------------
        // SEND EMAIL
        // -------------------------------------------------

        mailSender.send(message);
    }

    // =====================================================
    // SAFE VALUE
    // =====================================================

    private String safeValue(String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return "Customer";
        }

        return value;
    }

    // =====================================================
    // FORMAT AMOUNT
    // =====================================================

    private String formatAmount(BigDecimal amount) {

        if (amount == null) {
            return "0.00";
        }

        return String.format(
                "%.2f",
                amount
        );
    }
}
