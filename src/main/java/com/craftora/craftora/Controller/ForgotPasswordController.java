package com.craftora.craftora.Controller;

import com.craftora.craftora.Service.PasswordResetService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ForgotPasswordController {

    private final PasswordResetService passwordResetService;

    public ForgotPasswordController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    // ==============================
    // Forgot Password Page
    // ==============================
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    // ==============================
    // Send OTP
    // ==============================
    @PostMapping("/forgot-password")
    public String sendOtp(
            @RequestParam String email,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {

            passwordResetService.sendOtp(email);

            // Save email in session
            session.setAttribute("resetEmail", email.trim());

            return "redirect:/verify-otp";

        } catch (RuntimeException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            return "redirect:/forgot-password";
        }
    }

    // ==============================
    // OTP Verification Page
    // ==============================
    @GetMapping("/verify-otp")
    public String verifyOtpPage(HttpSession session) {

        Object resetEmail = session.getAttribute("resetEmail");

        if (resetEmail == null) {
            return "redirect:/forgot-password";
        }

        return "verify-otp";
    }

    // ==============================
    // Verify OTP
    // ==============================
    @PostMapping("/verify-otp")
    public String verifyOtp(
            @RequestParam String otp,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String email = (String) session.getAttribute("resetEmail");

        if (email == null) {
            return "redirect:/forgot-password";
        }

        try {

            passwordResetService.verifyOtp(email, otp);

            // OTP verified successfully
            session.setAttribute("otpVerified", true);

            return "redirect:/reset-password";

        } catch (RuntimeException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            return "redirect:/verify-otp";
        }
    }

    // ==============================
    // Reset Password Page
    // ==============================
    @GetMapping("/reset-password")
    public String resetPasswordPage(HttpSession session) {

        Object email = session.getAttribute("resetEmail");
        Object otpVerified = session.getAttribute("otpVerified");

        // User must verify OTP first
        if (email == null || !Boolean.TRUE.equals(otpVerified)) {
            return "redirect:/forgot-password";
        }

        return "reset-password";
    }

    // ==============================
    // Update Password
    // ==============================
    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String password,
            @RequestParam String confirmPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String email = (String) session.getAttribute("resetEmail");
        Object otpVerified = session.getAttribute("otpVerified");

        // Check OTP verification
        if (email == null || !Boolean.TRUE.equals(otpVerified)) {
            return "redirect:/forgot-password";
        }

        // ==============================
        // Check whether passwords match
        // ==============================
        if (!password.equals(confirmPassword)) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Passwords do not match. Please try again."
            );

            return "redirect:/reset-password";
        }

        // ==============================
        // Check minimum 8 characters
        // AND strong password requirements
        // ==============================
        if (!password.matches(
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%!]).{8,}$"
        )) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Password must be at least 8 characters and include an uppercase letter, lowercase letter, number, and special character."
            );

            return "redirect:/reset-password";
        }

        try {

            passwordResetService.resetPassword(
                    email,
                    password
            );

            // ==============================
            // Remove password reset session data
            // ==============================
            session.removeAttribute("resetEmail");
            session.removeAttribute("otpVerified");

            // ==============================
            // Success message
            // ==============================
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Your password has been reset successfully. Please login with your new password."
            );

            return "redirect:/login";

        } catch (RuntimeException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            return "redirect:/reset-password";
        }
    }
}