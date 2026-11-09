package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.User;
import com.craftora.craftora.Repository.UserRepository;
import com.craftora.craftora.Service.SystemSettingService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
public class AdminSettingsController {

    private final SystemSettingService systemSettingService;
    private final UserRepository userRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public AdminSettingsController(
            SystemSettingService systemSettingService,
            UserRepository userRepository) {

        this.systemSettingService =
                systemSettingService;

        this.userRepository =
                userRepository;
    }


    // =====================================================
    // GET LOGGED-IN USER
    // =====================================================

    private User getLoggedInUser(HttpSession session) {

        Object userIdObject =
                session.getAttribute("loggedInUserId");

        if (userIdObject == null) {
            return null;
        }

        try {

            Long userId =
                    Long.valueOf(
                            userIdObject.toString()
                    );

            return userRepository
                    .findById(userId)
                    .orElse(null);

        } catch (Exception e) {

            return null;
        }
    }


    // =====================================================
    // CHECK ADMIN ROLE
    // =====================================================

    private boolean isAdmin(User user) {

        if (user == null ||
                user.getRoles() == null) {

            return false;
        }

        return user.getRoles()
                .stream()
                .anyMatch(role ->
                        role.getRoleName()
                                .equalsIgnoreCase("ADMIN")
                );
    }


    // =====================================================
    // ADMIN SETTINGS PAGE
    // =====================================================

    @GetMapping("/admin/settings")
    public String showSettings(
            HttpSession session,
            Model model) {

        User user =
                getLoggedInUser(session);

        // -------------------------------------------------
        // CHECK LOGIN
        // -------------------------------------------------

        if (user == null) {
            return "redirect:/login";
        }


        // -------------------------------------------------
        // CHECK ADMIN ROLE
        // -------------------------------------------------

        if (!isAdmin(user)) {
            return "redirect:/login";
        }


        // -------------------------------------------------
        // GENERAL SETTINGS
        // -------------------------------------------------

        model.addAttribute(
                "systemName",
                getSettingOrDefault(
                        "SYSTEM_NAME",
                        "Craftora"
                )
        );

        model.addAttribute(
                "contactEmail",
                getSettingOrDefault(
                        "CONTACT_EMAIL",
                        "craftorasystem@gmail.com"
                )
        );

        model.addAttribute(
                "contactPhone",
                getSettingOrDefault(
                        "CONTACT_PHONE",
                        ""
                )
        );

        model.addAttribute(
                "businessAddress",
                getSettingOrDefault(
                        "BUSINESS_ADDRESS",
                        ""
                )
        );


        // -------------------------------------------------
        // PAYMENT SETTINGS
        // -------------------------------------------------

        model.addAttribute(
                "artisanRegistrationFee",
                systemSettingService
                        .getArtisanRegistrationFee()
        );

        model.addAttribute(
                "platformCommission",
                systemSettingService
                        .getPlatformCommission()
        );


        // -------------------------------------------------
        // NOTIFICATION SETTINGS
        // -------------------------------------------------

        model.addAttribute(
                "emailNotifications",
                systemSettingService
                        .isEmailNotificationsEnabled()
        );


        // -------------------------------------------------
        // SECURITY SETTINGS
        // -------------------------------------------------

        model.addAttribute(
                "minimumPasswordLength",
                systemSettingService
                        .getMinimumPasswordLength()
        );


        // -------------------------------------------------
        // ADMIN USER
        // -------------------------------------------------

        model.addAttribute(
                "user",
                user
        );


        return "admin-settings";
    }


    // =====================================================
    // SAVE ADMIN SETTINGS
    // =====================================================

    @PostMapping("/admin/settings/save")
    public String saveSettings(

            @RequestParam("systemName")
            String systemName,

            @RequestParam("contactEmail")
            String contactEmail,

            @RequestParam("contactPhone")
            String contactPhone,

            @RequestParam("businessAddress")
            String businessAddress,

            @RequestParam("artisanRegistrationFee")
            String artisanRegistrationFee,

            @RequestParam("platformCommission")
            String platformCommission,

            @RequestParam("emailNotifications")
            String emailNotifications,

            @RequestParam("minimumPasswordLength")
            String minimumPasswordLength,

            HttpSession session,

            RedirectAttributes redirectAttributes) {


        // -------------------------------------------------
        // CHECK LOGIN
        // -------------------------------------------------

        User user =
                getLoggedInUser(session);

        if (user == null) {
            return "redirect:/login";
        }


        // -------------------------------------------------
        // CHECK ADMIN
        // -------------------------------------------------

        if (!isAdmin(user)) {
            return "redirect:/login";
        }


        // -------------------------------------------------
        // CLEAN VALUES
        // -------------------------------------------------

        systemName =
                systemName == null
                        ? ""
                        : systemName.trim();

        contactEmail =
                contactEmail == null
                        ? ""
                        : contactEmail.trim();

        contactPhone =
                contactPhone == null
                        ? ""
                        : contactPhone.trim();

        businessAddress =
                businessAddress == null
                        ? ""
                        : businessAddress.trim();

        artisanRegistrationFee =
                artisanRegistrationFee == null
                        ? ""
                        : artisanRegistrationFee.trim();

        platformCommission =
                platformCommission == null
                        ? ""
                        : platformCommission.trim();

        minimumPasswordLength =
                minimumPasswordLength == null
                        ? ""
                        : minimumPasswordLength.trim();


        // =================================================
        // VALIDATE GENERAL SETTINGS
        // =================================================

        if (systemName.isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "System name cannot be empty."
            );

            return "redirect:/admin/settings";
        }


        if (contactEmail.isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Contact email cannot be empty."
            );

            return "redirect:/admin/settings";
        }


        // =================================================
        // VALIDATE ARTISAN REGISTRATION FEE
        // =================================================

        BigDecimal fee;

        try {

            fee =
                    new BigDecimal(
                            artisanRegistrationFee
                    );

            if (fee.compareTo(BigDecimal.ZERO) < 0) {

                throw new NumberFormatException();
            }

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please enter a valid Artisan Registration Fee."
            );

            return "redirect:/admin/settings";
        }


        // =================================================
        // VALIDATE PLATFORM COMMISSION
        // =================================================

        BigDecimal commission;

        try {

            commission =
                    new BigDecimal(
                            platformCommission
                    );

            if (commission.compareTo(
                    BigDecimal.ZERO) < 0
                    ||
                    commission.compareTo(
                            new BigDecimal("100")
                    ) > 0) {

                throw new NumberFormatException();
            }

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Platform Commission must be between 0% and 100%."
            );

            return "redirect:/admin/settings";
        }


        // =================================================
        // VALIDATE MINIMUM PASSWORD LENGTH
        // =================================================

        int passwordLength;

        try {

            passwordLength =
                    Integer.parseInt(
                            minimumPasswordLength
                    );

            if (passwordLength < 4 ||
                    passwordLength > 50) {

                throw new NumberFormatException();
            }

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Minimum Password Length must be between 4 and 50."
            );

            return "redirect:/admin/settings";
        }


        // =================================================
        // VALIDATE EMAIL NOTIFICATIONS
        // =================================================

        if (!emailNotifications.equalsIgnoreCase("ON")
                &&
                !emailNotifications.equalsIgnoreCase("OFF")) {

            emailNotifications = "ON";
        }


        // =================================================
        // SAVE GENERAL SETTINGS
        // =================================================

        systemSettingService.updateSetting(
                "SYSTEM_NAME",
                systemName
        );

        systemSettingService.updateSetting(
                "CONTACT_EMAIL",
                contactEmail
        );

        systemSettingService.updateSetting(
                "CONTACT_PHONE",
                contactPhone
        );

        systemSettingService.updateSetting(
                "BUSINESS_ADDRESS",
                businessAddress
        );


        // =================================================
        // SAVE PAYMENT SETTINGS
        // =================================================

        systemSettingService.updateSetting(
                "ARTISAN_REGISTRATION_FEE",
                fee.toPlainString()
        );

        systemSettingService.updateSetting(
                "PLATFORM_COMMISSION",
                commission.toPlainString()
        );


        // =================================================
        // SAVE NOTIFICATION SETTINGS
        // =================================================

        systemSettingService.updateSetting(
                "EMAIL_NOTIFICATIONS",
                emailNotifications.toUpperCase()
        );


        // =================================================
        // SAVE SECURITY SETTINGS
        // =================================================

        systemSettingService.updateSetting(
                "MINIMUM_PASSWORD_LENGTH",
                String.valueOf(passwordLength)
        );


        // =================================================
        // SUCCESS
        // =================================================

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Settings updated successfully."
        );


        return "redirect:/admin/settings";
    }


    // =====================================================
    // GET SETTING OR DEFAULT VALUE
    // =====================================================

    private String getSettingOrDefault(
            String settingKey,
            String defaultValue) {

        String value =
                systemSettingService
                        .getSetting(settingKey);

        if (value == null ||
                value.isBlank()) {

            return defaultValue;
        }

        return value;
    }
}