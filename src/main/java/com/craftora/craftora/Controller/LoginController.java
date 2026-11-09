package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.User;
import com.craftora.craftora.Service.ArtisanPaymentService;
import com.craftora.craftora.Service.EmailService;
import com.craftora.craftora.Service.LoginService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class LoginController {

    private final LoginService loginService;
    private final EmailService emailService;
    private final ArtisanPaymentService artisanPaymentService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public LoginController(
            LoginService loginService,
            EmailService emailService,
            ArtisanPaymentService artisanPaymentService) {

        this.loginService = loginService;
        this.emailService = emailService;
        this.artisanPaymentService = artisanPaymentService;
    }

    // =====================================================
    // LOGIN PAGE
    // =====================================================

    @GetMapping("/login")
    public String loginPage(Model model) {

        return "login";
    }

    // =====================================================
    // LOGIN
    // =====================================================

    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {

            // -------------------------------------------------
            // AUTHENTICATE USER
            // -------------------------------------------------

            User user =
                    loginService.authenticate(
                            email,
                            password
                    );

            // -------------------------------------------------
            // SEND LOGIN EMAIL
            // -------------------------------------------------

            try {

                emailService.sendLoginEmail(
                        user.getEmail(),
                        user.getFullName()
                );

            } catch (Exception e) {

                System.out.println(
                        "Login email could not be sent: "
                                + e.getMessage()
                );
            }

            // -------------------------------------------------
            // SAVE USER IN SESSION
            // -------------------------------------------------

            session.setAttribute(
                    "loggedInUserId",
                    user.getUserId()
            );

            session.setAttribute(
                    "loggedInUser",
                    user
            );

            // -------------------------------------------------
            // GET USER ROLES
            // -------------------------------------------------

            List<String> roles =
                    user.getRoles()
                            .stream()
                            .map(role -> role.getRoleName())
                            .toList();

            session.setAttribute(
                    "userRoles",
                    roles
            );

            // =================================================
            // SINGLE ROLE USER
            // =================================================

            if (roles.size() == 1) {

                String role = roles.get(0);

                session.setAttribute(
                        "selectedRole",
                        role
                );

                // -------------------------------------------------
                // CUSTOMER
                // -------------------------------------------------

                if (role.equalsIgnoreCase("CUSTOMER")) {

                    return "redirect:/customer-dashboard";
                }

                // -------------------------------------------------
                // ARTISAN
                // -------------------------------------------------

                if (role.equalsIgnoreCase("ARTISAN")) {

                    boolean paymentCompleted =
                            artisanPaymentService
                                    .isPaymentCompleted(
                                            user.getUserId()
                                    );

                    if (paymentCompleted) {

                        System.out.println(
                                "ARTISAN PAYMENT ALREADY COMPLETED"
                        );

                        System.out.println(
                                "REDIRECTING TO ARTISAN DASHBOARD"
                        );

                        return "redirect:/artisan-dashboard";

                    } else {

                        System.out.println(
                                "ARTISAN PAYMENT NOT COMPLETED"
                        );

                        System.out.println(
                                "REDIRECTING TO ARTISAN PAYMENT PAGE"
                        );

                        return "redirect:/artisan-payment";
                    }
                }

                // -------------------------------------------------
                // ADMIN
                // -------------------------------------------------

                if (role.equalsIgnoreCase("ADMIN")) {

                    return "redirect:/admin-dashboard";
                }
            }

            // =================================================
            // MULTIPLE ROLE USER
            // =================================================

            return "redirect:/select-role";

        } catch (RuntimeException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            return "redirect:/login";
        }
    }

    // =====================================================
    // SELECT ROLE PAGE
    // =====================================================

    @GetMapping("/select-role")
    public String selectRolePage(
            HttpSession session,
            Model model) {

        Object rolesObject =
                session.getAttribute("userRoles");

        if (!(rolesObject instanceof List<?>)) {

            return "redirect:/login";
        }

        List<?> rawRoles =
                (List<?>) rolesObject;

        boolean validRoles =
                rawRoles.stream()
                        .allMatch(
                                item ->
                                        item instanceof String
                        );

        if (!validRoles ||
                rawRoles.isEmpty()) {

            return "redirect:/login";
        }

        @SuppressWarnings("unchecked")
        List<String> roles =
                (List<String>) rawRoles;

        model.addAttribute(
                "roles",
                roles
        );

        return "select-role";
    }

    // =====================================================
    // SELECT ROLE
    // =====================================================

    @PostMapping("/select-role")
    public String selectRole(
            @RequestParam String role,
            HttpSession session) {

        Object rolesObject =
                session.getAttribute("userRoles");

        if (!(rolesObject instanceof List<?>)) {

            return "redirect:/login";
        }

        List<?> rawRoles =
                (List<?>) rolesObject;

        boolean validRoles =
                rawRoles.stream()
                        .allMatch(
                                item ->
                                        item instanceof String
                        );

        if (!validRoles) {

            return "redirect:/login";
        }

        @SuppressWarnings("unchecked")
        List<String> roles =
                (List<String>) rawRoles;

        // -------------------------------------------------
        // CHECK SELECTED ROLE EXISTS
        // -------------------------------------------------

        boolean roleExists =
                roles.stream()
                        .anyMatch(
                                r ->
                                        r.equalsIgnoreCase(role)
                        );

        if (!roleExists) {

            return "redirect:/login";
        }

        // -------------------------------------------------
        // SAVE SELECTED ROLE
        // -------------------------------------------------

        session.setAttribute(
                "selectedRole",
                role
        );

        // =================================================
        // ADMIN
        // =================================================

        if (role.equalsIgnoreCase("ADMIN")) {

            return "redirect:/admin-dashboard";
        }

        // =================================================
        // ARTISAN
        // =================================================

        if (role.equalsIgnoreCase("ARTISAN")) {

            /*
             * SPECIAL CASE:
             *
             * The system owner is both ADMIN and ARTISAN.
             * This account was created as the system owner
             * and does not need the normal artisan
             * registration/payment process.
             *
             * Therefore, if the logged-in user has both
             * ADMIN and ARTISAN roles, allow the user to
             * continue directly to the Artisan Dashboard.
             */

            boolean isAdmin =
                    roles.stream()
                            .anyMatch(
                                    r ->
                                            r.equalsIgnoreCase("ADMIN")
                            );

            boolean isArtisan =
                    roles.stream()
                            .anyMatch(
                                    r ->
                                            r.equalsIgnoreCase("ARTISAN")
                            );

            if (isAdmin && isArtisan) {

                System.out.println(
                        "OWNER ACCOUNT: ADMIN + ARTISAN"
                );

                System.out.println(
                        "BYPASSING ARTISAN REGISTRATION PAYMENT"
                );

                System.out.println(
                        "REDIRECTING TO ARTISAN DASHBOARD"
                );

                return "redirect:/artisan-dashboard";
            }

            // -------------------------------------------------
            // NORMAL ARTISAN
            // -------------------------------------------------

            Object userIdObject =
                    session.getAttribute(
                            "loggedInUserId"
                    );

            if (userIdObject instanceof Long) {

                Long userId =
                        (Long) userIdObject;

                boolean paymentCompleted =
                        artisanPaymentService
                                .isPaymentCompleted(
                                        userId
                                );

                if (paymentCompleted) {

                    System.out.println(
                            "ARTISAN PAYMENT ALREADY COMPLETED"
                    );

                    return "redirect:/artisan-dashboard";

                } else {

                    System.out.println(
                            "ARTISAN PAYMENT NOT COMPLETED"
                    );

                    return "redirect:/artisan-payment";
                }
            }

            return "redirect:/login";
        }

        // =================================================
        // CUSTOMER
        // =================================================

        if (role.equalsIgnoreCase("CUSTOMER")) {

            return "redirect:/customer-dashboard";
        }

        return "redirect:/login";
    }

    // =====================================================
// SWITCH ACCOUNT
// =====================================================

@GetMapping("/switch-account")
public String switchAccount(HttpSession session) {

    Object rolesObject = session.getAttribute("userRoles");

    if (!(rolesObject instanceof List<?>)) {
        return "redirect:/login";
    }

    List<?> rawRoles = (List<?>) rolesObject;

    boolean validRoles = rawRoles.stream()
            .allMatch(item -> item instanceof String);

    if (!validRoles || rawRoles.size() <= 1) {
        return "redirect:/artisan-dashboard";
    }

    return "redirect:/select-role";
}

    // =====================================================
    // LOGOUT
    // =====================================================

    @GetMapping("/logout")
    public String logout(
            HttpSession session) {

        session.invalidate();

        return "redirect:/login";
    }
}