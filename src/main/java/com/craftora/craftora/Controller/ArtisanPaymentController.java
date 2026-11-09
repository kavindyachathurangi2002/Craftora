package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.ArtisanPayment;
import com.craftora.craftora.Entity.ArtisanRequest;
import com.craftora.craftora.Repository.ArtisanPaymentRepository;
import com.craftora.craftora.Repository.ArtisanRequestRepository;
import com.craftora.craftora.Service.PayHereService;
import com.craftora.craftora.Service.PaymentEmailService;
import com.craftora.craftora.Service.PaymentReceiptPdfService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
public class ArtisanPaymentController {

    private final ArtisanRequestRepository artisanRequestRepository;
    private final ArtisanPaymentRepository artisanPaymentRepository;
    private final PayHereService payHereService;

    private final PaymentReceiptPdfService paymentReceiptPdfService;
    private final PaymentEmailService paymentEmailService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ArtisanPaymentController(
            ArtisanRequestRepository artisanRequestRepository,
            ArtisanPaymentRepository artisanPaymentRepository,
            PayHereService payHereService,
            PaymentReceiptPdfService paymentReceiptPdfService,
            PaymentEmailService paymentEmailService) {

        this.artisanRequestRepository =
                artisanRequestRepository;

        this.artisanPaymentRepository =
                artisanPaymentRepository;

        this.payHereService =
                payHereService;

        this.paymentReceiptPdfService =
                paymentReceiptPdfService;

        this.paymentEmailService =
                paymentEmailService;
    }

    // =====================================================
    // ARTISAN PAYMENT PAGE
    // =====================================================

    @GetMapping("/artisan-payment")
    public String showArtisanPaymentPage(
            HttpSession session,
            Model model) {

        // -------------------------------------------------
        // CHECK LOGIN
        // -------------------------------------------------

        Object userIdObject =
                session.getAttribute("loggedInUserId");

        if (!(userIdObject instanceof Long)) {

            return "redirect:/login";
        }

        Long userId =
                (Long) userIdObject;

        // -------------------------------------------------
        // FIND APPROVED ARTISAN REQUEST
        // -------------------------------------------------

        List<ArtisanRequest> requests =
                artisanRequestRepository
                        .findByUserId(userId);

        ArtisanRequest approvedRequest =
                requests.stream()
                        .filter(request ->
                                request.getStatus() != null
                                        &&
                                request.getStatus()
                                        .equalsIgnoreCase("APPROVED"))
                        .findFirst()
                        .orElse(null);

        // -------------------------------------------------
        // CHECK APPROVED REQUEST
        // -------------------------------------------------

        if (approvedRequest == null) {

            model.addAttribute(
                    "errorMessage",
                    "Your Artisan registration has not been approved yet."
            );

            return "artisan-payment";
        }

        // -------------------------------------------------
        // CHECK REGISTRATION FEE
        // -------------------------------------------------

        if (approvedRequest.getRegistrationFee() == null) {

            model.addAttribute(
                    "errorMessage",
                    "Registration fee is not available for this request."
            );

            return "artisan-payment";
        }

        // -------------------------------------------------
        // SEND DATA TO ARTISAN PAYMENT PAGE
        // -------------------------------------------------

        model.addAttribute(
                "artisanRequest",
                approvedRequest
        );

        model.addAttribute(
                "registrationFee",
                approvedRequest.getRegistrationFee()
        );

        return "artisan-payment";
    }

    // =====================================================
    // START PAYHERE PAYMENT
    // =====================================================

    @PostMapping("/artisan-payment/payhere")
    public String startPayHerePayment(
            HttpSession session,
            Model model) {

        // -------------------------------------------------
        // CHECK LOGIN
        // -------------------------------------------------

        Object userIdObject =
                session.getAttribute("loggedInUserId");

        if (!(userIdObject instanceof Long)) {

            return "redirect:/login";
        }

        Long userId =
                (Long) userIdObject;

        // -------------------------------------------------
        // FIND APPROVED ARTISAN REQUEST
        // -------------------------------------------------

        List<ArtisanRequest> requests =
                artisanRequestRepository
                        .findByUserId(userId);

        ArtisanRequest approvedRequest =
                requests.stream()
                        .filter(request ->
                                request.getStatus() != null
                                        &&
                                request.getStatus()
                                        .equalsIgnoreCase("APPROVED"))
                        .findFirst()
                        .orElse(null);

        if (approvedRequest == null) {

            return "redirect:/artisan-payment";
        }


        
        // Check whether this registration request is already paid
        boolean alreadyPaid =
                artisanPaymentRepository
                        .findFirstByRequestIdAndPaymentStatusIgnoreCaseOrderByCreatedAtDesc(
                                approvedRequest.getRequestId(),
                                "PAID"
                        )
                        .isPresent();

        if (alreadyPaid) {
            model.addAttribute(
                    "errorMessage",
                    "Your artisan registration fee has already been paid."
            );

            return "artisan-payment";
        }


        
        // -------------------------------------------------
        // GET REGISTRATION FEE
        // -------------------------------------------------

        BigDecimal amount =
                approvedRequest.getRegistrationFee();

        if (amount == null) {

            return "redirect:/artisan-payment";
        }


// -------------------------------------------------
// REUSE PENDING PAYMENT OR CREATE A NEW ONE
// -------------------------------------------------

ArtisanPayment payment =
        artisanPaymentRepository
                .findFirstByRequestIdAndPaymentStatusIgnoreCaseOrderByCreatedAtDesc(
                        approvedRequest.getRequestId(),
                        "PENDING"
                )
                .orElse(null);

String orderId;

if (payment != null) {

    // Reuse the existing pending payment
    orderId = payment.getTransactionId();

    // Ensure the pending payment belongs to this user
    if (payment.getUserId() == null
            || !payment.getUserId().equals(userId)
            || orderId == null
            || orderId.isBlank()) {

        model.addAttribute(
                "errorMessage",
                "Unable to continue the pending payment. Please contact the administrator."
        );

        model.addAttribute("artisanRequest", approvedRequest);
        model.addAttribute("registrationFee", amount);

        return "artisan-payment";
    }

    // Update the amount in case the registration fee changed
    payment.setAmount(amount);

    artisanPaymentRepository.saveAndFlush(payment);

    System.out.println("EXISTING PENDING PAYMENT REUSED");
    System.out.println("ORDER ID : " + orderId);

} else {

    // Create a new payment only when no pending payment exists
    orderId = "ARTISAN-" + userId + "-"
            + System.currentTimeMillis();

    payment = new ArtisanPayment();

    payment.setRequestId(approvedRequest.getRequestId());
    payment.setUserId(userId);
    payment.setAmount(amount);
    payment.setPaymentMethod("PAYHERE");
    payment.setTransactionId(orderId);
    payment.setPaymentStatus("PENDING");
    payment.setCreatedAt(LocalDateTime.now());

    artisanPaymentRepository.saveAndFlush(payment);

    System.out.println("NEW PENDING PAYMENT CREATED");
    System.out.println("ORDER ID : " + orderId);
}

        System.out.println(
                "PAYMENT RECORD CREATED : PENDING"
        );

        // -------------------------------------------------
        // CREATE PAYHERE CHECKOUT DATA
        // -------------------------------------------------

        Map<String, String> checkoutData =
                payHereService.createCheckoutData(
                        orderId,
                        approvedRequest.getFirstName(),
                        approvedRequest.getLastName(),
                        approvedRequest.getEmail(),
                        approvedRequest.getContactNumber(),
                        "Craftora Artisan Registration",
                        "Sri Lanka",
                        amount
                );

        // -------------------------------------------------
        // SEND TO PAYHERE REDIRECT PAGE
        // -------------------------------------------------

        model.addAttribute(
                "checkoutUrl",
                payHereService.getCheckoutUrl()
        );

        model.addAttribute(
                "checkoutData",
                checkoutData
        );

        return "payhere-redirect";
    }

    // =====================================================
    // PAYHERE PAYMENT SUCCESS
    // =====================================================
    //
    // synchronized is used so that the same payment success
    // request cannot be processed twice at the same time.
    // =====================================================

    @GetMapping("/artisan-payment/success")
    public synchronized String paymentSuccess(
            @RequestParam("order_id") String orderId,
            Model model) {

        System.out.println(
                "================================================="
        );

        System.out.println(
                "PAYHERE SUCCESS URL REACHED"
        );

        System.out.println(
                "RAW ORDER ID FROM PAYHERE : "
                        + orderId
        );

        // -------------------------------------------------
        // CLEAN DUPLICATE ORDER ID
        // -------------------------------------------------

        if (orderId != null
                && orderId.contains(",")) {

            orderId =
                    orderId
                            .split(",")[0]
                            .trim();
        }

        System.out.println(
                "CLEAN ORDER ID : "
                        + orderId
        );

        System.out.println(
                "SEARCHING PAYMENT FOR ORDER : "
                        + orderId
        );

        // -------------------------------------------------
        // FIND PAYMENT RECORD
        // -------------------------------------------------

        ArtisanPayment payment =
                artisanPaymentRepository
                        .findByTransactionId(orderId)
                        .orElse(null);

        if (payment == null) {

            System.out.println(
                    "ERROR: PAYMENT RECORD NOT FOUND"
            );

            model.addAttribute(
                    "errorMessage",
                    "Payment record could not be found."
            );

            return "artisan-payment-success";
        }

        System.out.println(
                "PAYMENT RECORD FOUND"
        );

        System.out.println(
                "PAYMENT ID : "
                        + payment.getPaymentId()
        );

        System.out.println(
                "CURRENT PAYMENT STATUS : "
                        + payment.getPaymentStatus()
        );

        // -------------------------------------------------
        // CHECK WHETHER ALREADY PAID
        // -------------------------------------------------

        if ("PAID".equalsIgnoreCase(
                payment.getPaymentStatus())) {

            System.out.println(
                    "PAYMENT IS ALREADY PAID"
            );

            model.addAttribute(
                    "paymentStatus",
                    "PAID"
            );

            model.addAttribute(
                    "message",
                    "Payment was already completed successfully."
            );

            return "artisan-payment-success";
        }

        // -------------------------------------------------
        // FIND ARTISAN REQUEST
        // -------------------------------------------------

        ArtisanRequest artisanRequest =
                artisanRequestRepository
                        .findById(
                                payment.getRequestId()
                        )
                        .orElse(null);

        if (artisanRequest == null) {

            System.out.println(
                    "ERROR: ARTISAN REQUEST NOT FOUND"
            );

            model.addAttribute(
                    "errorMessage",
                    "Artisan details could not be found."
            );

            return "artisan-payment-success";
        }

        // -------------------------------------------------
        // GET ARTISAN DETAILS
        // -------------------------------------------------

        String artisanName =
                artisanRequest.getFirstName()
                        + " "
                        + artisanRequest.getLastName();

        String artisanEmail =
                artisanRequest.getEmail();

        // -------------------------------------------------
        // PAYMENT DATE
        // -------------------------------------------------

        LocalDateTime paidAt =
                LocalDateTime.now();

        // =================================================
        // UPDATE PAYMENT TO PAID
        // =================================================

        payment.setPaymentStatus(
                "PAID"
        );

        payment.setPaidAt(
                paidAt
        );

        payment.setTransactionId(
                orderId
        );

        // -------------------------------------------------
        // SAVE AND FLUSH IMMEDIATELY
        // -------------------------------------------------

        artisanPaymentRepository.saveAndFlush(
                payment
        );

        System.out.println(
                "PAYMENT UPDATED SUCCESSFULLY"
        );

        System.out.println(
                "STATUS : "
                        + payment.getPaymentStatus()
        );

        // =================================================
        // GENERATE PDF RECEIPT
        // =================================================

        byte[] pdfReceipt;

        try {

            pdfReceipt =
                    paymentReceiptPdfService.generateReceipt(
                            orderId,
                            artisanName,
                            artisanEmail,
                            payment.getAmount(),
                            "PAID",
                            paidAt
                    );

            System.out.println(
                    "PDF RECEIPT GENERATED SUCCESSFULLY"
            );

        } catch (Exception e) {

            System.out.println(
                    "ERROR: PDF RECEIPT GENERATION FAILED"
            );

            e.printStackTrace();

            model.addAttribute(
                    "paymentStatus",
                    "PAID"
            );

            model.addAttribute(
                    "message",
                    "Payment was successful, but the PDF receipt could not be generated."
            );

            return "artisan-payment-success";
        }

        // =================================================
        // SEND EMAIL WITH PDF
        // =================================================

        try {

            paymentEmailService.sendPaymentReceipt(
                    artisanEmail,
                    artisanName,
                    pdfReceipt
            );

            System.out.println(
                    "PAYMENT RECEIPT EMAIL SENT SUCCESSFULLY"
            );

            model.addAttribute(
                    "message",
                    "Payment successful! Your payment receipt has been generated and sent to your email."
            );

        } catch (Exception e) {

            System.out.println(
                    "ERROR: PAYMENT RECEIPT EMAIL FAILED"
            );

            e.printStackTrace();

            /*
             * Payment is already PAID in the database.
             * Only email sending failed.
             */

            model.addAttribute(
                    "message",
                    "Payment was successful, but the receipt email could not be sent."
            );
        }

        // =================================================
        // SUCCESS PAGE
        // =================================================

        model.addAttribute(
                "paymentStatus",
                "PAID"
        );

        return "artisan-payment-success";
    }

    // =====================================================
    // PAYHERE PAYMENT CANCEL
    // =====================================================

    @GetMapping("/artisan-payment/cancel")
    public String paymentCancel(
            @RequestParam(
                    value = "order_id",
                    required = false
            )
            String orderId,
            Model model) {

        if (orderId != null
                && orderId.contains(",")) {

            orderId =
                    orderId
                            .split(",")[0]
                            .trim();
        }

        model.addAttribute(
                "orderId",
                orderId
        );

        model.addAttribute(
                "message",
                "Your payment was cancelled."
        );

        return "artisan-payment-cancel";
    }
}