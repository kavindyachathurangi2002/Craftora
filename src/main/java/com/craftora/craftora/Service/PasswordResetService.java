package com.craftora.craftora.Service;

import com.craftora.craftora.Entity.PasswordResetOtp;
import com.craftora.craftora.Entity.User;
import com.craftora.craftora.Repository.PasswordResetOtpRepository;
import com.craftora.craftora.Repository.UserRepository;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    private final SecureRandom secureRandom =
            new SecureRandom();

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetOtpRepository otpRepository,
            EmailService emailService) {

        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.emailService = emailService;
    }

    // ==============================
    // Send OTP
    // ==============================
    @Transactional
    public void sendOtp(String email) {

        email = email.trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "This email address is not registered with Craftora."
                        )
                );

        // Delete previous OTPs
        otpRepository.deleteByUserUserId(user.getUserId());

        // Generate 6-digit OTP
        String otp = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        // Create new OTP
        PasswordResetOtp passwordResetOtp =
                new PasswordResetOtp();

        passwordResetOtp.setUser(user);

        // Store encrypted OTP
        passwordResetOtp.setOtp(
                passwordEncoder.encode(otp)
        );

        // OTP valid for 5 minutes
        passwordResetOtp.setExpiresAt(
                LocalDateTime.now().plusMinutes(5)
        );

        passwordResetOtp.setVerified(false);

        passwordResetOtp.setCreatedAt(
                LocalDateTime.now()
        );

        // Save OTP
        otpRepository.save(passwordResetOtp);

        // Send OTP email
        emailService.sendPasswordResetOtpEmail(
                user.getEmail(),
                user.getFullName(),
                otp
        );
    }


    // ==============================
    // Verify OTP
    // ==============================
    @Transactional
    public void verifyOtp(
            String email,
            String enteredOtp) {

        email = email.trim();
        enteredOtp = enteredOtp.trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "This email address is not registered with Craftora."
                        )
                );

        PasswordResetOtp resetOtp =
                otpRepository
                        .findTopByUserUserIdAndVerifiedFalseOrderByCreatedAtDesc(
                                user.getUserId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No valid OTP found. Please request a new OTP."
                                )
                        );

        // ==============================
        // Check OTP expiration
        // ==============================

        if (LocalDateTime.now().isAfter(
                resetOtp.getExpiresAt())) {

            otpRepository.delete(resetOtp);

            throw new RuntimeException(
                    "Your OTP has expired. Please request a new OTP."
            );
        }

        // ==============================
        // Check OTP
        // ==============================

        if (!passwordEncoder.matches(
                enteredOtp,
                resetOtp.getOtp())) {

            throw new RuntimeException(
                    "Incorrect OTP. Please try again."
            );
        }

        // ==============================
        // OTP is correct
        // ==============================

        resetOtp.setVerified(true);

        otpRepository.save(resetOtp);
    }


    // ==============================
    // Reset Password
    // ==============================
    @Transactional
    public void resetPassword(
            String email,
            String newPassword) {

        email = email.trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User account was not found."
                        )
                );

        // IMPORTANT:
        // Find the VERIFIED OTP
        PasswordResetOtp resetOtp =
                otpRepository
                        .findTopByUserUserIdAndVerifiedTrueOrderByCreatedAtDesc(
                                user.getUserId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "OTP verification is required."
                                )
                        );

        // ==============================
        // Check OTP expiration
        // ==============================

        if (LocalDateTime.now().isAfter(
                resetOtp.getExpiresAt())) {

            otpRepository.delete(resetOtp);

            throw new RuntimeException(
                    "Your OTP has expired. Please request a new OTP."
            );
        }

        // ==============================
        // Update Password
        // ==============================

        user.setPassword(
                passwordEncoder.encode(newPassword)
        );

        userRepository.save(user);

        // ==============================
        // Delete OTP after successful
        // password reset
        // ==============================

        otpRepository.delete(resetOtp);
    }
}