package com.craftora.craftora.Service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendRegistrationEmail(String to, String name) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Welcome to Craftora");

        message.setText(
                "Hello " + name + ",\n\n"
                + "Your registration on Craftora was successful.\n\n"
                + "Thank you for joining Craftora!\n\n"
                + "Best regards,\n"
                + "Craftora Team"
        );

        mailSender.send(message);
    }

    public void sendLoginEmail(String to, String name) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Craftora Login Successful");

        message.setText(
                "Hello " + name + ",\n\n"
                + "You have successfully logged in to your Craftora account.\n\n"
                + "If this was not you, please contact Craftora support.\n\n"
                + "Best regards,\n"
                + "Craftora Team"
        );

        mailSender.send(message);
    }

    // Artisan registration submission email
    public void sendArtisanSubmissionEmail(String to, String name) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Craftora Artisan Registration Submitted");

        message.setText(
                "Hello " + name + ",\n\n"
                + "Your Artisan registration on Craftora has been submitted successfully.\n\n"
                + "Your application is currently pending admin approval.\n"
                + "The Craftora admin will review your details and certificates.\n\n"
                + "You will receive another email once your application has been reviewed.\n\n"
                + "Thank you for joining Craftora!\n\n"
                + "Best regards,\n"
                + "Craftora Team"
        );

        mailSender.send(message);
    }

    public void sendPasswordResetOtpEmail(String to, String name, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Craftora Password Reset OTP");

        message.setText(
                "Hello " + name + ",\n\n"
                + "We received a request to reset your Craftora account password.\n\n"
                + "Your password reset OTP is:\n\n"
                + otp + "\n\n"
                + "This OTP is valid for 5 minutes.\n\n"
                + "If you did not request a password reset, please ignore this email.\n\n"
                + "Best regards,\n"
                + "Craftora Team"
        );

        mailSender.send(message);
    }

    // Artisan registration approval email
    public void sendArtisanApprovalEmail(String to, String name) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Craftora Artisan Application Approved");

        message.setText(
                "Hello " + name + ",\n\n"
                + "Congratulations!\n\n"
                + "Your Artisan registration application on Craftora "
                + "has been APPROVED by the administrator.\n\n"
                + "You can now log in to your Craftora account "
                + "and complete the Artisan Registration Fee Payment.\n\n"
                + "Please use the following link to login:\n"
                + "http://localhost:8080/login\n\n"
                + "After logging in, please continue as Artisan "
                + "and complete the registration fee payment.\n\n"
                + "Your Artisan Dashboard will be available "
                + "after the registration fee payment is successfully completed.\n\n"
                + "Thank you for joining Craftora!\n\n"
                + "Best regards,\n"
                + "Craftora Team"
        );

        mailSender.send(message);
    }

    // Low Stock Alert Email
    public void sendLowStockAlertEmail(
            String to,
            String artisanName,
            String productName,
            Integer stockQuantity) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Craftora - Low Stock Alert");

        message.setText(
                "Hello " + artisanName + ",\n\n"
                + "Your product is running low on stock.\n\n"
                + "Product Name: " + productName + "\n"
                + "Current Stock: " + stockQuantity + "\n\n"
                + "The stock quantity has reached 5 or below.\n"
                + "Please update your stock when necessary.\n\n"
                + "Thank you,\n"
                + "Craftora Team"
        );

        mailSender.send(message);
    }

    // Contact Form Reply Email
public void sendContactReplyEmail(
        String to,
        String customerName,
        String originalSubject,
        String replyMessage) {

    SimpleMailMessage message = new SimpleMailMessage();

    message.setTo(to);
    message.setSubject("Craftora - Reply to Your Message");

    message.setText(
            "Hello " + customerName + ",\n\n"
            + "Thank you for contacting Craftora.\n\n"
            + "Regarding your message:\n"
            + originalSubject + "\n\n"
            + "Our response:\n\n"
            + replyMessage + "\n\n"
            + "If you have any further questions, "
            + "please feel free to contact us.\n\n"
            + "Best regards,\n"
            + "Craftora Team"
    );

    mailSender.send(message);
}
// Live Event Discussion Invitation Email
public void sendEventDiscussionInvitationEmail(
        String to,
        String artisanName,
        String eventType,
        String chatLink) {

    SimpleMailMessage message = new SimpleMailMessage();

    String eventName;
    String article;

    if ("WORKSHOP".equalsIgnoreCase(eventType)) {
        eventName = "Workshop";
        article = "a";
    } else if ("EXHIBITION".equalsIgnoreCase(eventType)) {
        eventName = "Exhibition";
        article = "an";
    } else {
        eventName = "Competition";
        article = "a";
    }

    message.setTo(to);

    message.setSubject(
            "Craftora – " + eventName + " Discussion Invitation"
    );

    message.setText(
            "Hello " + artisanName + ",\n\n"

            + "A discussion has been opened regarding "
            + article + " proposed " + eventName
            + " on Craftora.\n\n"

            + "All approved artisans are invited to join "
            + "the live discussion and share their ideas "
            + "and suggestions.\n\n"

            + "Please note: The discussion will remain open "
            + "until the administrator closes it and is "
            + "expected to be closed within 24 hours.\n\n"

            + "Important Notice:\n"
            + "All messages shared during this discussion "
            + "will be saved in the Craftora system and will "
            + "be visible to participating approved artisans.\n\n"

            + "Therefore, we kindly encourage everyone to "
            + "communicate respectfully, maintain professional "
            + "conduct, and share ideas and suggestions "
            + "constructively.\n\n"

            + "Join Live Discussion:\n"
            + chatLink + "\n\n"

            + "We look forward to hearing your ideas and "
            + "contributions.\n\n"

            + "Best regards,\n"
            + "Craftora Team"
    );

    mailSender.send(message);
}
}