package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Event;
import com.craftora.craftora.Entity.EventPayment;
import com.craftora.craftora.Entity.EventRegistration;
import com.craftora.craftora.Entity.User;

import com.craftora.craftora.Repository.EventPaymentRepository;
import com.craftora.craftora.Repository.EventRegistrationRepository;
import com.craftora.craftora.Repository.EventRepository;
import com.craftora.craftora.Repository.UserRepository;

import com.craftora.craftora.Service.EventRegistrationEmailService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;


@Controller
public class EventPaymentController {

    private final EventPaymentRepository eventPaymentRepository;

    private final EventRegistrationRepository
            eventRegistrationRepository;

    private final EventRepository eventRepository;

    private final UserRepository userRepository;

    private final EventRegistrationEmailService
            eventRegistrationEmailService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public EventPaymentController(
            EventPaymentRepository eventPaymentRepository,
            EventRegistrationRepository eventRegistrationRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            EventRegistrationEmailService eventRegistrationEmailService) {

        this.eventPaymentRepository =
                eventPaymentRepository;

        this.eventRegistrationRepository =
                eventRegistrationRepository;

        this.eventRepository =
                eventRepository;

        this.userRepository =
                userRepository;

        this.eventRegistrationEmailService =
                eventRegistrationEmailService;
    }


    // =====================================================
    // PAYMENT SUCCESS
    // =====================================================

    @GetMapping("/event-payment/success")
    public synchronized String paymentSuccess(

            @RequestParam("order_id") String[] orderIds,

            HttpSession session,

            Model model) {


        // =================================================
        // GET CLEAN ORDER ID
        // =================================================

        String cleanOrderId = null;


        if (orderIds != null) {

            for (String value : orderIds) {

                if (value != null
                        && !value.trim().isEmpty()) {

                    cleanOrderId =
                            value.trim();

                    break;
                }
            }
        }


        // =================================================
        // CONSOLE DEBUG
        // =================================================

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "EVENT PAYMENT SUCCESS"
        );

        System.out.println(
                "Received Order IDs:"
        );


        if (orderIds != null) {

            for (String value : orderIds) {

                System.out.println(
                        " - " + value
                );
            }
        }


        System.out.println(
                "Clean Order ID : "
                        + cleanOrderId
        );

        System.out.println(
                "=========================================="
        );


        // =================================================
        // ORDER ID NOT FOUND
        // =================================================

        if (cleanOrderId == null) {

            model.addAttribute(
                    "message",
                    "Payment order ID could not be found."
            );

            return "event-payment-failed";
        }


        // =================================================
        // FIND PAYMENT RECORD
        // =================================================

        EventPayment payment =
                eventPaymentRepository
                        .findByTransactionId(
                                cleanOrderId
                        )
                        .orElse(null);


        // =================================================
        // PAYMENT RECORD NOT FOUND
        // =================================================

        if (payment == null) {

            System.out.println(
                    "=========================================="
            );

            System.out.println(
                    "PAYMENT RECORD NOT FOUND"
            );

            System.out.println(
                    "Transaction ID searched : "
                            + cleanOrderId
            );

            System.out.println(
                    "=========================================="
            );


            model.addAttribute(
                    "message",
                    "Payment record could not be found."
            );

            return "event-payment-failed";
        }


        // =================================================
        // ALREADY PAID
        // =================================================

        /*
         * If PayHere or the browser calls the success URL
         * again, do NOT send the email/PDF again.
         */

        if ("PAID".equalsIgnoreCase(
                payment.getPaymentStatus())) {


            Event event =
                    eventRepository
                            .findById(
                                    payment.getEventId()
                            )
                            .orElse(null);


            EventRegistration registration =
                    eventRegistrationRepository
                            .findById(
                                    payment.getRegistrationId()
                            )
                            .orElse(null);


            model.addAttribute(
                    "event",
                    event
            );


            model.addAttribute(
                    "registration",
                    registration
            );


            model.addAttribute(
                    "payment",
                    payment
            );


            if (registration != null) {

                model.addAttribute(
                        "selectedRole",
                        registration
                                .getRegistrationRole()
                );
            }


            return "event-payment-success";
        }


        // =================================================
        // FIND EVENT REGISTRATION
        // =================================================

        EventRegistration registration =
                eventRegistrationRepository
                        .findById(
                                payment.getRegistrationId()
                        )
                        .orElse(null);


        if (registration == null) {

            model.addAttribute(
                    "message",
                    "Event registration could not be found."
            );

            return "event-payment-failed";
        }


        // =================================================
        // FIND EVENT
        // =================================================

        Event event =
                eventRepository
                        .findById(
                                payment.getEventId()
                        )
                        .orElse(null);


        if (event == null) {

            model.addAttribute(
                    "message",
                    "Event could not be found."
            );

            return "event-payment-failed";
        }


        // =================================================
        // FIND USER
        // =================================================

        User user =
                userRepository
                        .findById(
                                payment.getUserId()
                        )
                        .orElse(null);


        if (user == null) {

            model.addAttribute(
                    "message",
                    "User could not be found."
            );

            return "event-payment-failed";
        }


        // =================================================
        // MARK PAYMENT AS PAID
        // =================================================

        LocalDateTime paidAt =
                LocalDateTime.now();


        payment.setPaymentStatus(
                "PAID"
        );


        payment.setPaidAt(
                paidAt
        );


        eventPaymentRepository.save(
                payment
        );


        // =================================================
        // CONFIRM REGISTRATION
        // =================================================

        registration.setRegistrationStatus(
                "CONFIRMED"
        );


        registration.setPaymentStatus(
                "PAID"
        );


        eventRegistrationRepository.save(
                registration
        );


        // =================================================
        // SEND PAYMENT RECEIPT EMAIL + PDF
        // =================================================

        try {

            eventRegistrationEmailService
                    .sendRegistrationPaymentReceipt(

                            user,

                            event,

                            registration
                                    .getRegistrationRole(),

                            cleanOrderId,

                            payment.getAmount(),

                            paidAt
                    );


            // ---------------------------------------------
            // EMAIL SUCCESS DEBUG
            // ---------------------------------------------

            System.out.println(
                    "=========================================="
            );

            System.out.println(
                    "PAYMENT RECEIPT EMAIL SENT SUCCESSFULLY"
            );

            System.out.println(
                    "Recipient : "
                            + user.getEmail()
            );

            System.out.println(
                    "Order ID : "
                            + cleanOrderId
            );

            System.out.println(
                    "PDF : ATTACHED"
            );

            System.out.println(
                    "=========================================="
            );


        } catch (Exception e) {


            // ---------------------------------------------
            // EMAIL FAILURE DEBUG
            // ---------------------------------------------

            System.out.println(
                    "=========================================="
            );

            System.out.println(
                    "PAYMENT RECEIPT EMAIL FAILED"
            );

            System.out.println(
                    "=========================================="
            );

            System.out.println(
                    "Recipient : "
                            + user.getEmail()
            );

            System.out.println(
                    "Order ID : "
                            + cleanOrderId
            );

            System.out.println(
                    "Event : "
                            + event.getEventTitle()
            );

            System.out.println(
                    "Error : "
                            + e.getMessage()
            );

            e.printStackTrace();

            System.out.println(
                    "=========================================="
            );


            /*
             * Payment and registration remain successful
             * even if email sending fails.
             */
        }


        // =================================================
        // SUCCESS PAGE DATA
        // =================================================

        model.addAttribute(
                "event",
                event
        );


        model.addAttribute(
                "registration",
                registration
        );


        model.addAttribute(
                "payment",
                payment
        );


        model.addAttribute(
                "email",
                user.getEmail()
        );


        model.addAttribute(
                "selectedRole",
                registration
                        .getRegistrationRole()
        );


        // =================================================
        // RETURN SUCCESS PAGE
        // =================================================

        return "event-payment-success";
    }


    // =====================================================
    // PAYMENT CANCEL
    // =====================================================

    @GetMapping("/event-payment/cancel")
    public String paymentCancel(

            @RequestParam("order_id")
            String orderId,

            Model model) {


        // =================================================
        // CLEAN ORDER ID
        // =================================================

        String cleanOrderId =
                orderId;


        if (cleanOrderId != null
                && cleanOrderId.contains(",")) {

            cleanOrderId =
                    cleanOrderId
                            .split(",")[0]
                            .trim();
        }


        // =================================================
        // FIND PAYMENT
        // =================================================

        EventPayment payment =
                eventPaymentRepository
                        .findByTransactionId(
                                cleanOrderId
                        )
                        .orElse(null);


        // =================================================
        // UPDATE REGISTRATION
        // =================================================

        if (payment != null) {

            EventRegistration registration =
                    eventRegistrationRepository
                            .findById(
                                    payment.getRegistrationId()
                            )
                            .orElse(null);


            if (registration != null) {

                registration.setRegistrationStatus(
                        "PENDING_PAYMENT"
                );


                registration.setPaymentStatus(
                        "PENDING"
                );


                eventRegistrationRepository.save(
                        registration
                );
            }
        }


        // =================================================
        // CANCEL MESSAGE
        // =================================================

        model.addAttribute(
                "message",
                "Your payment was cancelled. "
                        + "Your event registration has "
                        + "not been confirmed."
        );


        return "event-payment-cancel";
    }


    // =====================================================
    // PAYMENT NOTIFY
    // =====================================================

    @GetMapping("/event-payment/notify")
    public String paymentNotify() {

        return "OK";
    }

}