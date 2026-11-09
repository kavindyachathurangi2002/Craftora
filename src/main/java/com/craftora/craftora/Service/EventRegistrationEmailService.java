package com.craftora.craftora.Service;

import com.craftora.craftora.Entity.Event;
import com.craftora.craftora.Entity.User;

import jakarta.mail.internet.MimeMessage;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EventRegistrationEmailService {

    private final JavaMailSender mailSender;

    private final EventPaymentReceiptPdfService
            eventPaymentReceiptPdfService;


    public EventRegistrationEmailService(
            JavaMailSender mailSender,
            EventPaymentReceiptPdfService
                    eventPaymentReceiptPdfService) {

        this.mailSender = mailSender;

        this.eventPaymentReceiptPdfService =
                eventPaymentReceiptPdfService;
    }


    // =====================================================
    // SEND PAID EVENT REGISTRATION PAYMENT RECEIPT
    // =====================================================

    public void sendRegistrationPaymentReceipt(
            User user,
            Event event,
            String registrationRole,
            String orderId,
            BigDecimal amount,
            LocalDateTime paidAt) {

        try {

            // -------------------------------------------------
            // GENERATE PDF RECEIPT
            // -------------------------------------------------

            byte[] pdfBytes =
                    eventPaymentReceiptPdfService
                            .generateEventReceipt(
                                    orderId,
                                    user.getFullName(),
                                    user.getEmail(),
                                    registrationRole,
                                    event,
                                    amount,
                                    "PAID",
                                    paidAt
                            );


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
            // RECIPIENT
            // -------------------------------------------------

            helper.setTo(
                    user.getEmail()
            );


            // -------------------------------------------------
            // SUBJECT
            // -------------------------------------------------

            helper.setSubject(
                    "Event Registration Confirmed - "
                            + event.getEventTitle()
            );


            // -------------------------------------------------
            // EVENT DATE
            // -------------------------------------------------

            String eventDate = "-";


            if (event.getEventDate() != null) {

                eventDate =
                        event.getEventDate()
                                .format(
                                        DateTimeFormatter
                                                .ofPattern(
                                                        "dd-MM-yyyy"
                                                )
                                );
            }


            // -------------------------------------------------
            // EMAIL BODY
            // -------------------------------------------------

            String body =
                    "Dear "
                            + safe(user.getFullName())
                            + ",\n\n"

                            + "Your registration for the following "
                            + "Craftora event has been successfully "
                            + "confirmed.\n\n"

                            + "EVENT DETAILS\n"
                            + "--------------------------------\n"

                            + "Event Title : "
                            + safe(event.getEventTitle())
                            + "\n"

                            + "Event Type  : "
                            + safe(event.getEventType())
                            + "\n"

                            + "Event Date  : "
                            + eventDate
                            + "\n"

                            + "Role        : "
                            + safe(registrationRole)
                            + "\n\n"

                            + "PAYMENT DETAILS\n"
                            + "--------------------------------\n"

                            + "Payment Method : PayHere\n"

                            + "Transaction Reference : "
                            + safe(orderId)
                            + "\n"

                            + "Payment Status : PAID\n"

                            + "Amount : LKR "
                            + formatAmount(amount)
                            + "\n\n"

                            + "Your payment has been completed "
                            + "successfully and your event "
                            + "registration is confirmed.\n\n"

                            + "Your payment receipt is attached "
                            + "to this email as a PDF.\n\n"

                            + "Thank you for registering with "
                            + "Craftora.\n\n"

                            + "Craftora Handicraft Development "
                            + "and Community Management System";


            helper.setText(
                    body
            );


            // -------------------------------------------------
            // ATTACH PDF
            // -------------------------------------------------

            ByteArrayResource pdfResource =
                    new ByteArrayResource(
                            pdfBytes
                    );


            String fileName =
                    "Craftora-Event-Payment-Receipt-"
                            + event.getEventId()
                            + "-"
                            + orderId
                            + ".pdf";


            helper.addAttachment(
                    fileName,
                    pdfResource
            );


            // -------------------------------------------------
            // SEND EMAIL
            // -------------------------------------------------

            mailSender.send(
                    message
            );


            System.out.println(
                    "=========================================="
            );

            System.out.println(
                    "PAID EVENT EMAIL SENT SUCCESSFULLY"
            );

            System.out.println(
                    "Recipient : "
                            + user.getEmail()
            );

            System.out.println(
                    "Order ID : "
                            + orderId
            );

            System.out.println(
                    "PDF : ATTACHED"
            );

            System.out.println(
                    "=========================================="
            );


        } catch (Exception e) {


            // -------------------------------------------------
            // SHOW ACTUAL ERROR IN CONSOLE
            // -------------------------------------------------

            System.out.println(
                    "=========================================="
            );

            System.out.println(
                    "PAID EVENT EMAIL FAILED"
            );

            System.out.println(
                    "=========================================="
            );

            System.out.println(
                    "Recipient : "
                            + (
                                user != null
                                ? user.getEmail()
                                : "NULL"
                              )
            );

            System.out.println(
                    "Order ID : "
                            + orderId
            );

            System.out.println(
                    "Event : "
                            + (
                                event != null
                                ? event.getEventTitle()
                                : "NULL"
                              )
            );

            System.out.println(
                    "Error : "
                            + e.getMessage()
            );

            System.out.println(
                    "Cause : "
                            + (
                                e.getCause() != null
                                ? e.getCause().getMessage()
                                : "No additional cause"
                              )
            );

            e.printStackTrace();

            System.out.println(
                    "=========================================="
            );


            throw new RuntimeException(
                    "Failed to send event registration "
                            + "payment receipt email.",
                    e
            );
        }
    }


    // =====================================================
    // SEND FREE EVENT REGISTRATION CONFIRMATION
    // =====================================================

    public void sendFreeRegistrationConfirmation(
            User user,
            Event event,
            String registrationRole,
            Long registrationId) {

        try {

            MimeMessage message =
                    mailSender.createMimeMessage();


            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true
                    );


            helper.setTo(
                    user.getEmail()
            );


            helper.setSubject(
                    "Event Registration Confirmed - "
                            + event.getEventTitle()
            );


            String eventDate = "-";


            if (event.getEventDate() != null) {

                eventDate =
                        event.getEventDate()
                                .format(
                                        DateTimeFormatter
                                                .ofPattern(
                                                        "dd-MM-yyyy"
                                                )
                                );
            }


            String registrationNumber =
                    "REG-"
                            + String.format(
                                    "%05d",
                                    registrationId
                            );


            String body =
                    "Dear "
                            + safe(user.getFullName())
                            + ",\n\n"

                            + "Your registration for the following "
                            + "Craftora event has been successfully "
                            + "confirmed.\n\n"

                            + "EVENT DETAILS\n"
                            + "--------------------------------\n"

                            + "Registration No : "
                            + registrationNumber
                            + "\n"

                            + "Event Title     : "
                            + safe(event.getEventTitle())
                            + "\n"

                            + "Event Type      : "
                            + safe(event.getEventType())
                            + "\n"

                            + "Event Date      : "
                            + eventDate
                            + "\n"

                            + "Role            : "
                            + safe(registrationRole)
                            + "\n\n"

                            + "Registration Status : FREE\n\n"

                            + "Your event registration has been "
                            + "successfully completed.\n\n"

                            + "Please keep this email for your "
                            + "future reference.\n\n"

                            + "Thank you for registering with "
                            + "Craftora.\n\n"

                            + "Craftora Handicraft Development "
                            + "and Community Management System";


            helper.setText(
                    body
            );


            mailSender.send(
                    message
            );


            System.out.println(
                    "=========================================="
            );

            System.out.println(
                    "FREE EVENT EMAIL SENT SUCCESSFULLY"
            );

            System.out.println(
                    "Recipient : "
                            + user.getEmail()
            );

            System.out.println(
                    "Registration No : "
                            + registrationNumber
            );

            System.out.println(
                    "=========================================="
            );


        } catch (Exception e) {


            System.out.println(
                    "=========================================="
            );

            System.out.println(
                    "FREE EVENT EMAIL FAILED"
            );

            System.out.println(
                    "=========================================="
            );

            System.out.println(
                    "Recipient : "
                            + (
                                user != null
                                ? user.getEmail()
                                : "NULL"
                              )
            );

            System.out.println(
                    "Error : "
                            + e.getMessage()
            );

            e.printStackTrace();

            System.out.println(
                    "=========================================="
            );


            throw new RuntimeException(
                    "Failed to send free event "
                            + "registration confirmation email.",
                    e
            );
        }
    }


    // =====================================================
    // SAFE STRING
    // =====================================================

    private String safe(
            String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return "-";
        }

        return value;
    }


    // =====================================================
    // FORMAT AMOUNT
    // =====================================================

    private String formatAmount(
            BigDecimal amount) {

        if (amount == null) {

            return "0.00";
        }

        return String.format(
                "%.2f",
                amount
        );
    }

}