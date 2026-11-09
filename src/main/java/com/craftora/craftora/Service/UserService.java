package com.craftora.craftora.Service;

import com.craftora.craftora.Entity.Customer;
import com.craftora.craftora.Entity.Role;
import com.craftora.craftora.Entity.User;
import com.craftora.craftora.Repository.CustomerRepository;
import com.craftora.craftora.Repository.RoleRepository;
import com.craftora.craftora.Repository.UserRepository;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            CustomerRepository customerRepository,
            EmailService emailService) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.customerRepository = customerRepository;
        this.emailService = emailService;
    }

    @Transactional
    public User registerCustomer(
            String firstName,
            String lastName,
            String email,
            String phone,
            String password) {

        // Remove unnecessary spaces
        firstName = firstName.trim();
        lastName = lastName.trim();
        email = email.trim();
        phone = phone.trim();

        /*
         * Check whether this email already has
         * a user account.
         */
        User user = userRepository.findByEmail(email).orElse(null);

        /*
         * Find CUSTOMER role.
         */
        Role customerRole = roleRepository
                .findByRoleName("CUSTOMER")
                .orElseThrow(() ->
                        new RuntimeException(
                                "CUSTOMER role was not found."
                        )
                );

        /*
         * Existing user
         */
        if (user != null) {

            /*
             * Check whether this user already
             * has the CUSTOMER role.
             */
            boolean alreadyCustomer = user.getRoles()
                    .stream()
                    .anyMatch(role ->
                            role.getRoleName()
                                    .equalsIgnoreCase("CUSTOMER")
                    );

            /*
             * Same email cannot register
             * as Customer more than once.
             */
            if (alreadyCustomer) {
                throw new RuntimeException(
                        "This email is already registered as a Customer. " +
                        "You can only have one Customer account per email."
                );
            }

            /*
             * Existing Artisan user can also
             * register as Customer.
             *
             * Add CUSTOMER role to the
             * existing user account.
             */
            user.getRoles().add(customerRole);

            User savedUser = userRepository.save(user);

            /*
             * Create Customer profile record.
             *
             * Registration details such as
             * name, email and phone are already
             * stored in the users table.
             */
            createCustomerProfile(savedUser);

            /*
             * Send Customer registration email.
             *
             * Email failure will not cancel
             * the registration.
             */
            try {
                emailService.sendRegistrationEmail(
                        savedUser.getEmail(),
                        savedUser.getFullName()
                );
            } catch (Exception e) {
                System.out.println(
                        "Customer registration email could not be sent: "
                                + e.getMessage()
                );
            }

            return savedUser;
        }

        /*
         * New user registration
         */
        user = new User();

        user.setFullName(
                firstName + " " + lastName
        );

        user.setEmail(email);

        user.setPhone(phone);

        /*
         * Encrypt password using BCrypt.
         */
        user.setPassword(
                passwordEncoder.encode(password)
        );

        /*
         * Save registration date and time.
         */
        user.setCreatedAt(
                LocalDateTime.now()
        );

        /*
         * Assign CUSTOMER role.
         */
        user.getRoles().add(customerRole);

        /*
         * Save user and role relationship.
         */
        User savedUser = userRepository.save(user);

        /*
         * Create Customer profile record.
         */
        createCustomerProfile(savedUser);

        /*
         * Send registration email.
         *
         * Email failure will NOT cancel
         * the registration.
         */
        try {
            emailService.sendRegistrationEmail(
                    savedUser.getEmail(),
                    savedUser.getFullName()
            );
        } catch (Exception e) {
            System.out.println(
                    "Registration email could not be sent: "
                            + e.getMessage()
            );
        }

        return savedUser;
    }

    /*
     * Create a Customer profile for the user.
     *
     * This creates the record in the
     * customers table automatically.
     */
    private void createCustomerProfile(User user) {

        /*
         * Prevent duplicate Customer profile.
         */
        if (customerRepository
                .existsByUserUserId(user.getUserId())) {
            return;
        }

        Customer customer = new Customer();

        customer.setUser(user);

        /*
         * These are optional at registration.
         * Customer can add them later from
         * the Customer Profile page.
         */
        customer.setProfileImage(null);
        customer.setAddress(null);
        customer.setCity(null);

        customer.setCreatedAt(
                LocalDateTime.now()
        );

        customerRepository.save(customer);
    }
}