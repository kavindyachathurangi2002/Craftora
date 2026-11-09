package com.craftora.craftora.Service;

import com.craftora.craftora.Entity.User;
import com.craftora.craftora.Repository.UserRepository;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public LoginService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User authenticate(String email, String password) {

        email = email.trim();

        User user = userRepository
                .findByEmail(email)
                .orElse(null);

        // User not found
        if (user == null) {
            throw new RuntimeException(
                    "Invalid email or password."
            );
        }

        // Password check
        if (!passwordEncoder.matches(
                password,
                user.getPassword())) {

            throw new RuntimeException(
                    "Invalid email or password."
            );
        }

        // No roles assigned
        if (user.getRoles() == null ||
                user.getRoles().isEmpty()) {

            throw new RuntimeException(
                    "No role is assigned to this account."
            );
        }

        return user;
    }
}