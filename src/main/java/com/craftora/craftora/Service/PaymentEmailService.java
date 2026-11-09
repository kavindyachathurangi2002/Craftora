package com.craftora.craftora.Service;

import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class PaymentEmailService {

    @Autowired
    private JavaMailSender mailSender;


    // =====================================================
    // SEND PAYMENT RECEIPT EMAIL
    // =====================================================

    public void sendPaymentReceipt(
            String toEmail,
            String artisanName,
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
                "Craftora - Artisan Registration Payment Receipt"
        );


        // -------------------------------------------------
        // EMAIL BODY
        // -------------------------------------------------

        String emailBody =
                "Dear " + artisanName + ",\n\n"
                + "Your Artisan Registration payment has been completed successfully.\n\n"
                + "Payment Amount: LKR 2,500.00\n"
                + "Payment Method: PayHere\n"
                + "Payment Status: PAID\n\n"
                + "Please find your payment receipt attached to this email as a PDF.\n\n"
                + "Thank you for registering as an artisan with Craftora.\n\n"
                + "Best Regards,\n"
                + "Craftora Team";

        helper.setText(emailBody);


        // -------------------------------------------------
        // ATTACH PDF RECEIPT
        // -------------------------------------------------

        helper.addAttachment(
                "Craftora-Artisan-Payment-Receipt.pdf",
                new ByteArrayResource(pdfReceipt)
        );


        // -------------------------------------------------
        // SEND EMAIL
        // -------------------------------------------------

        mailSender.send(message);
    }
}