package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    // Find latest unverified OTP
    Optional<PasswordResetOtp> findTopByUserUserIdAndVerifiedFalseOrderByCreatedAtDesc(Long userId);

    // Find latest verified OTP
    Optional<PasswordResetOtp> findTopByUserUserIdAndVerifiedTrueOrderByCreatedAtDesc(Long userId);

    // Delete all OTPs belonging to a user
    void deleteByUserUserId(Long userId);
}