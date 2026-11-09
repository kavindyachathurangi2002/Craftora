package com.craftora.craftora.Controller;


import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Entity.ArtisanCertificate;
import com.craftora.craftora.Entity.ArtisanRequest;
import com.craftora.craftora.Entity.ArtisanPayment;
import com.craftora.craftora.Entity.Role;
import com.craftora.craftora.Entity.User;
import com.craftora.craftora.Entity.ContactMessage;

import com.craftora.craftora.Repository.ArtisanCertificateRepository;
import com.craftora.craftora.Repository.ArtisanRepository;
import com.craftora.craftora.Repository.ArtisanRequestRepository;
import com.craftora.craftora.Repository.ArtisanPaymentRepository;
import com.craftora.craftora.Repository.RoleRepository;
import com.craftora.craftora.Repository.UserRepository;
import com.craftora.craftora.Repository.ContactMessageRepository;

import com.craftora.craftora.Service.AdminService;
import com.craftora.craftora.Service.EmailService;

import jakarta.servlet.http.HttpSession;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Controller
public class AdminController {


private final ArtisanRequestRepository artisanRequestRepository;
private final ArtisanPaymentRepository artisanPaymentRepository;
private final ArtisanRepository artisanRepository;
private final ArtisanCertificateRepository artisanCertificateRepository;
private final UserRepository userRepository;
private final RoleRepository roleRepository;
private final EmailService emailService;
private final AdminService adminService;
private final ContactMessageRepository contactMessageRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public AdminController(
            ArtisanRequestRepository artisanRequestRepository,
            ArtisanPaymentRepository artisanPaymentRepository,
            ArtisanRepository artisanRepository,
            ArtisanCertificateRepository artisanCertificateRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            EmailService emailService,
            ContactMessageRepository contactMessageRepository,
            AdminService adminService) {

        this.artisanRequestRepository = artisanRequestRepository;
        this.artisanPaymentRepository = artisanPaymentRepository;
        this.artisanRepository = artisanRepository;
        this.artisanCertificateRepository = artisanCertificateRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.emailService = emailService;
        this.adminService = adminService;
        this.contactMessageRepository = contactMessageRepository;
    }


    // =====================================================
    // GET LOGGED-IN USER
    // =====================================================

    private User getLoggedInUser(HttpSession session) {

        Object userIdObject =
                session.getAttribute("loggedInUserId");


        if (userIdObject != null) {

            try {

                Long userId =
                        Long.valueOf(
                                userIdObject.toString()
                        );


                User user =
                        userRepository
                                .findById(userId)
                                .orElse(null);


                if (user != null) {
                    return user;
                }


            } catch (Exception e) {

                System.out.println(
                        "Could not read loggedInUserId: "
                                + e.getMessage()
                );
            }
        }


        // -------------------------------------------------
        // FALLBACK
        // -------------------------------------------------

        Object loggedInUser =
                session.getAttribute("loggedInUser");


        if (loggedInUser instanceof User) {

            User user =
                    (User) loggedInUser;


            if (user.getUserId() != null) {
                return user;
            }
        }


        return null;
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
    // ADMIN DASHBOARD
    // =====================================================

    @GetMapping("/admin-dashboard")
    public String adminDashboard(
            HttpSession session,
            Model model) {

        User user =
                getLoggedInUser(session);


        if (user == null) {

            return "redirect:/login";
        }


        if (!isAdmin(user)) {

            return "redirect:/login";
        }


        model.addAttribute(
                "artisanCount",
                adminService.getApprovedArtisanCount()
        );


        model.addAttribute(
                "eventCount",
                adminService.getEventCount()
        );


        model.addAttribute(
                "totalRevenue",
                adminService.getTotalRevenue()
        );


        // -------------------------------------------------
        // LOAD ADMIN'S ARTISAN PROFILE
        // -------------------------------------------------
        // The admin account can also have an Artisan profile.
        // The profile image is stored in the same Artisan record.
        Artisan artisan =
                artisanRepository
                        .findByUserUserId(
                                user.getUserId()
                        )
                        .orElse(null);


        model.addAttribute(
                "artisan",
                artisan
        );


        model.addAttribute(
                "recentRequests",
                adminService.getRecentArtisanRequests()
        );


        model.addAttribute(
                "pendingRequestCount",
                adminService.getPendingArtisanRequestCount()
        );


        // Send admin user to dashboard

        model.addAttribute(
                "user",
                user
        );
model.addAttribute(
        "contactMessageCount",
        contactMessageRepository.count()
);

        return "admin-dashboard";
    }
// =====================================================
// VIEW CONTACT MESSAGES
// =====================================================

@GetMapping("/admin/contact-messages")
public String viewContactMessages(
        HttpSession session,
        Model model) {

    User user = getLoggedInUser(session);

    if (user == null || !isAdmin(user)) {
        return "redirect:/login";
    }

    List<ContactMessage> messages =
            contactMessageRepository.findAll();

    model.addAttribute("contactMessages", messages);

    return "admin-contact-messages";
}

    // =====================================================
    // ADMIN PROFILE
    // =====================================================

    @GetMapping("/admin/profile")
    public String adminProfile(
            HttpSession session,
            Model model) {


        System.out.println(
                "========== ADMIN PROFILE =========="
        );


        // -------------------------------------------------
        // GET LOGGED-IN USER
        // -------------------------------------------------

        User user =
                getLoggedInUser(session);


        System.out.println(
                "Logged in user = "
                        + user
        );


        // -------------------------------------------------
        // CHECK LOGIN
        // -------------------------------------------------

        if (user == null) {

            System.out.println(
                    "NO USER FOUND - REDIRECTING TO LOGIN"
            );


            return "redirect:/login";
        }


        // -------------------------------------------------
        // CHECK ADMIN ROLE
        // -------------------------------------------------

        if (!isAdmin(user)) {

            System.out.println(
                    "USER DOES NOT HAVE ADMIN ROLE"
            );


            return "redirect:/login";
        }


        // -------------------------------------------------
        // FIND ARTISAN PROFILE
        // -------------------------------------------------

        Artisan artisan =
                artisanRepository
                        .findByUserUserId(
                                user.getUserId()
                        )
                        .orElse(null);


        System.out.println(
                "Artisan profile = "
                        + artisan
        );


        // -------------------------------------------------
        // SEND DATA TO HTML
        // -------------------------------------------------

        model.addAttribute(
                "user",
                user
        );


        model.addAttribute(
                "artisan",
                artisan
        );


        System.out.println(
                "ADMIN PROFILE PAGE LOADING"
        );


        return "admin-profile";
    }


    // =====================================================
    // ADMIN / ARTISAN PROFILE PHOTO UPLOAD
    // =====================================================

    @PostMapping("/admin/profile/photo")
    public String uploadAdminProfilePhoto(
            @RequestParam("profileImage")
            MultipartFile profileImage,

            HttpSession session,

            RedirectAttributes redirectAttributes) {


        // -------------------------------------------------
        // GET LOGGED-IN USER
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
        // FIND ARTISAN PROFILE
        // -------------------------------------------------

        Artisan artisan =
                artisanRepository
                        .findByUserUserId(
                                user.getUserId()
                        )
                        .orElse(null);


        if (artisan == null) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Artisan profile was not found."
            );


            return "redirect:/admin/profile";
        }


        // -------------------------------------------------
        // CHECK IMAGE
        // -------------------------------------------------

        if (profileImage == null ||
                profileImage.isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please select a profile photo."
            );


            return "redirect:/admin/profile";
        }


        // -------------------------------------------------
        // CHECK IMAGE TYPE
        // -------------------------------------------------

        String contentType =
                profileImage.getContentType();


        if (contentType == null ||
                (!contentType.equalsIgnoreCase("image/jpeg")
                        && !contentType.equalsIgnoreCase("image/jpg")
                        && !contentType.equalsIgnoreCase("image/png"))) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Only JPG, JPEG and PNG images are allowed."
            );


            return "redirect:/admin/profile";
        }


        // -------------------------------------------------
        // MAXIMUM FILE SIZE = 5 MB
        // -------------------------------------------------

        long maxFileSize =
                5 * 1024 * 1024;


        if (profileImage.getSize() > maxFileSize) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Profile photo must be smaller than 5 MB."
            );


            return "redirect:/admin/profile";
        }


        // -------------------------------------------------
        // SAVE IMAGE
        // -------------------------------------------------

        try {

            Path uploadDirectory =
                    Paths.get(
                            "uploads/artisan-profiles"
                    );


            Files.createDirectories(
                    uploadDirectory
            );


            String originalFileName =
                    profileImage
                            .getOriginalFilename();


            String extension = "";


            if (originalFileName != null &&
                    originalFileName.contains(".")) {

                extension =
                        originalFileName.substring(
                                originalFileName.lastIndexOf(".")
                        );
            }


            String fileName =
                    UUID.randomUUID()
                            + extension;


            Path filePath =
                    uploadDirectory.resolve(
                            fileName
                    );


            Files.copy(
                    profileImage.getInputStream(),
                    filePath
            );


            // -------------------------------------------------
            // SAVE IMAGE PATH TO ARTISAN TABLE
            // -------------------------------------------------

            artisan.setProfileImage(
                    "uploads/artisan-profiles/"
                            + fileName
            );


            artisanRepository.save(
                    artisan
            );


            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Profile photo updated successfully."
            );


        } catch (IOException e) {

            e.printStackTrace();


            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Could not upload profile photo. Please try again."
            );
        }


        return "redirect:/admin/profile";
    }


    // =====================================================
    // MANAGE ARTISANS
    // =====================================================

    @GetMapping("/admin/manage-artisans")
    public String manageArtisans(
            HttpSession session,
            Model model) {


        User user =
                getLoggedInUser(session);


        if (user == null ||
                !isAdmin(user)) {

            return "redirect:/login";
        }


        List<ArtisanRequest> pendingRequests =
                artisanRequestRepository
                        .findByStatus("PENDING");


        List<Artisan> approvedArtisans =
                artisanRepository
                        .findByStatusIgnoreCase("APPROVED");


        model.addAttribute(
                "pendingRequests",
                pendingRequests
        );


        model.addAttribute(
                "approvedArtisans",
                approvedArtisans
        );


        return "manage-artisans";
    }


    // =====================================================
    // VIEW APPROVED ARTISAN DETAILS
    // =====================================================

    @GetMapping("/admin/approved-artisan/{id}")
    public String viewApprovedArtisan(

            @PathVariable("id")
            Long id,

            HttpSession session,

            Model model,

            RedirectAttributes redirectAttributes) {

        User user = getLoggedInUser(session);

        if (user == null || !isAdmin(user)) {
            return "redirect:/login";
        }

        Artisan artisan =
                artisanRepository.findById(id).orElse(null);

        if (artisan == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Artisan not found."
            );
            return "redirect:/admin/manage-artisans";
        }

        if (artisan.getStatus() == null ||
                !artisan.getStatus().equalsIgnoreCase("APPROVED")) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This artisan is not approved."
            );
            return "redirect:/admin/manage-artisans";
        }

        ArtisanPayment registrationPayment =
                artisanPaymentRepository
                        .findByUserId(artisan.getUser().getUserId())
                        .stream()
                        .filter(payment ->
                                payment.getPaymentStatus() != null &&
                                payment.getPaymentStatus()
                                        .equalsIgnoreCase("PAID")
                        )
                        .findFirst()
                        .orElse(null);

        List<ArtisanCertificate> certificates =
                new java.util.ArrayList<>();

        List<ArtisanRequest> requests =
                artisanRequestRepository
                        .findByUserId(artisan.getUser().getUserId());

        ArtisanRequest approvedRequest =
                requests.stream()
                        .filter(request ->
                                request.getStatus() != null &&
                                request.getStatus()
                                        .equalsIgnoreCase("APPROVED")
                        )
                        .findFirst()
                        .orElse(null);

        if (approvedRequest != null) {
            certificates =
                    artisanCertificateRepository
                            .findByRequestId(
                                    approvedRequest.getRequestId()
                            );
        }

        model.addAttribute("artisan", artisan);
        model.addAttribute("artisanRequest", approvedRequest);

        String firstName = "-";
        String lastName = "-";

        if (approvedRequest != null) {
            if (approvedRequest.getFirstName() != null
                    && !approvedRequest.getFirstName().trim().isEmpty()) {
                firstName = approvedRequest.getFirstName();
            }
            if (approvedRequest.getLastName() != null
                    && !approvedRequest.getLastName().trim().isEmpty()) {
                lastName = approvedRequest.getLastName();
            }
        }

        if (("-".equals(firstName) || "-".equals(lastName))
                && artisan.getUser() != null
                && artisan.getUser().getFullName() != null
                && !artisan.getUser().getFullName().trim().isEmpty()) {
            String fullName = artisan.getUser().getFullName().trim();
            String[] nameParts = fullName.split("\\s+", 2);
            if ("-".equals(firstName)) {
                firstName = nameParts[0];
            }
            if ("-".equals(lastName) && nameParts.length > 1) {
                lastName = nameParts[1];
            }
        }

        model.addAttribute("firstName", firstName);
        model.addAttribute("lastName", lastName);
        model.addAttribute("registrationPayment", registrationPayment);
        model.addAttribute("certificates", certificates);

        return "approved-artisan-details";
    }


    // =====================================================
    // VIEW ARTISAN REQUEST
    // =====================================================

    @GetMapping("/admin/artisan-request/{id}")
    public String viewArtisanRequest(

            @PathVariable("id")
            Long id,

            HttpSession session,

            Model model,

            RedirectAttributes redirectAttributes) {


        User user =
                getLoggedInUser(session);


        if (user == null ||
                !isAdmin(user)) {

            return "redirect:/login";
        }


        ArtisanRequest request =
                artisanRequestRepository
                        .findById(id)
                        .orElse(null);


        if (request == null) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Artisan request not found."
            );


            return "redirect:/admin/manage-artisans";
        }


        List<ArtisanCertificate> certificates =
                artisanCertificateRepository
                        .findByRequestId(id);


        model.addAttribute(
                "request",
                request
        );


        model.addAttribute(
                "certificates",
                certificates
        );


        return "artisan-request-details";
    }


    // =====================================================
    // OPEN ARTISAN CERTIFICATE
    // =====================================================

    @GetMapping("/admin/artisan-certificate/{fileName:.+}")
    public ResponseEntity<Resource> openArtisanCertificate(
            @PathVariable("fileName")
            String fileName) {

        try {

            // Find the certificate record using the original file name.
            Optional<ArtisanCertificate> certificateOptional =
                    artisanCertificateRepository
                            .findFirstByFileName(fileName);

            if (certificateOptional.isEmpty()) {

                System.out.println(
                        "Certificate database record not found: "
                                + fileName
                );

                return ResponseEntity.notFound().build();
            }

            ArtisanCertificate certificate =
                    certificateOptional.get();

            // Get the actual physical file path saved during upload.
            String storedFilePath = certificate.getFilePath();

            System.out.println(
                    "Certificate database file path: "
                            + storedFilePath
            );

            if (storedFilePath == null ||
                    storedFilePath.trim().isEmpty()) {

                System.out.println(
                        "Certificate file path is empty."
                );

                return ResponseEntity.notFound().build();
            }

            Path filePath =
                    Paths.get(storedFilePath)
                            .toAbsolutePath()
                            .normalize();

            System.out.println(
                    "Actual certificate file: "
                            + filePath
            );

            if (!Files.exists(filePath) ||
                    !Files.isRegularFile(filePath) ||
                    !Files.isReadable(filePath)) {

                System.out.println(
                        "Certificate physical file not found: "
                                + filePath
                );

                return ResponseEntity.notFound().build();
            }

            Resource resource =
                    new UrlResource(filePath.toUri());

            if (!resource.exists() ||
                    !resource.isReadable()) {

                return ResponseEntity.notFound().build();
            }

            String contentType =
                    Files.probeContentType(filePath);

            if (contentType == null) {
                contentType =
                        MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(
                            MediaType.parseMediaType(contentType)
                    )
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\""
                                    + certificate.getFileName()
                                    + "\""
                    )
                    .body(resource);

        } catch (Exception e) {

            e.printStackTrace();

            System.out.println(
                    "Error opening certificate: "
                            + e.getMessage()
            );

            return ResponseEntity
                    .internalServerError()
                    .build();
        }
    }


    // =====================================================
    // APPROVE ARTISAN
    // =====================================================

    @PostMapping("/admin/artisan-request/{id}/approve")
    @Transactional
    public String approveArtisan(

            @PathVariable("id")
            Long id,

            HttpSession session,

            RedirectAttributes redirectAttributes) {


        User admin =
                getLoggedInUser(session);


        if (admin == null ||
                !isAdmin(admin)) {

            return "redirect:/login";
        }


        ArtisanRequest request =
                artisanRequestRepository
                        .findById(id)
                        .orElse(null);


        if (request == null) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Artisan request not found."
            );


            return "redirect:/admin/manage-artisans";
        }


        if (!"PENDING".equalsIgnoreCase(
                request.getStatus())) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This artisan request has already been reviewed."
            );


            return "redirect:/admin/manage-artisans";
        }


        // -------------------------------------------------
        // UPDATE REQUEST
        // -------------------------------------------------

        request.setStatus("APPROVED");


        request.setReviewedAt(
                LocalDateTime.now()
        );


        artisanRequestRepository.save(
                request
        );


        // -------------------------------------------------
        // UPDATE ARTISAN
        // -------------------------------------------------

        Artisan artisan =
                artisanRepository
                        .findByUserUserId(
                                request.getUserId()
                        )
                        .orElse(null);


        if (artisan != null) {

            artisan.setStatus("APPROVED");


            artisanRepository.save(
                    artisan
            );
        }


        // -------------------------------------------------
        // FIND USER
        // -------------------------------------------------

        User user =
                userRepository
                        .findById(
                                request.getUserId()
                        )
                        .orElse(null);


        if (user == null) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "User account could not be found."
            );


            return "redirect:/admin/manage-artisans";
        }


        // -------------------------------------------------
        // FIND ARTISAN ROLE
        // -------------------------------------------------

        Role artisanRole =
                roleRepository
                        .findByRoleName("ARTISAN")
                        .orElse(null);


        if (artisanRole == null) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "ARTISAN role was not found in the database."
            );


            return "redirect:/admin/manage-artisans";
        }


        // -------------------------------------------------
        // ADD ARTISAN ROLE
        // -------------------------------------------------

        boolean alreadyArtisan =
                user.getRoles()
                        .stream()
                        .anyMatch(role ->
                                role.getRoleName()
                                        .equalsIgnoreCase(
                                                "ARTISAN"
                                        )
                        );


        if (!alreadyArtisan) {

            user.getRoles()
                    .add(artisanRole);


            userRepository.save(
                    user
            );
        }


        // -------------------------------------------------
        // SEND APPROVAL EMAIL
        // -------------------------------------------------

        try {

            emailService.sendArtisanApprovalEmail(
                    request.getEmail(),

                    request.getFirstName()
                            + " "
                            + request.getLastName()
            );

        } catch (Exception e) {

            System.out.println(
                    "Approval email could not be sent: "
                            + e.getMessage()
            );
        }


        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Artisan application approved successfully."
        );


        return "redirect:/admin/manage-artisans";
    }


    // =====================================================
    // REJECT ARTISAN
    // =====================================================

    @PostMapping("/admin/artisan-request/{id}/reject")
    @Transactional
    public String rejectArtisan(

            @PathVariable("id")
            Long id,

            @RequestParam("adminNote")
            String adminNote,

            HttpSession session,

            RedirectAttributes redirectAttributes) {


        User admin =
                getLoggedInUser(session);


        if (admin == null ||
                !isAdmin(admin)) {

            return "redirect:/login";
        }


        ArtisanRequest request =
                artisanRequestRepository
                        .findById(id)
                        .orElse(null);


        if (request == null) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Artisan request not found."
            );


            return "redirect:/admin/manage-artisans";
        }


        if (!"PENDING".equalsIgnoreCase(
                request.getStatus())) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This artisan request has already been reviewed."
            );


            return "redirect:/admin/manage-artisans";
        }


        // -------------------------------------------------
        // UPDATE REQUEST
        // -------------------------------------------------

        request.setStatus("REJECTED");


        request.setAdminNote(
                adminNote == null
                        ? ""
                        : adminNote.trim()
        );


        request.setReviewedAt(
                LocalDateTime.now()
        );


        artisanRequestRepository.save(
                request
        );


        // -------------------------------------------------
        // UPDATE ARTISAN
        // -------------------------------------------------

        Artisan artisan =
                artisanRepository
                        .findByUserUserId(
                                request.getUserId()
                        )
                        .orElse(null);


        if (artisan != null) {

            artisan.setStatus("REJECTED");


            artisanRepository.save(
                    artisan
            );
        }


        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Artisan application rejected."
        );


        return "redirect:/admin/manage-artisans";
    }


    // =====================================================
    // OPEN EDIT ADMIN PROFILE
    // =====================================================

    @GetMapping("/admin/profile/edit")
    public String editAdminProfile(
            HttpSession session,
            Model model) {


        User user =
                getLoggedInUser(session);


        if (user == null ||
                !isAdmin(user)) {

            return "redirect:/login";
        }


        Artisan artisan =
                artisanRepository
                        .findByUserUserId(
                                user.getUserId()
                        )
                        .orElse(null);


        model.addAttribute(
                "user",
                user
        );


        model.addAttribute(
                "artisan",
                artisan
        );


        return "admin-profile-edit";
    }


    // =====================================================
    // SAVE EDITED ADMIN PROFILE
    // =====================================================

    @PostMapping("/admin/profile/edit")
    @Transactional
    public String saveAdminProfile(

            @RequestParam("fullName")
            String fullName,

            @RequestParam("phone")
            String phone,

            @RequestParam("craftType")
            String craftType,

            @RequestParam("location")
            String location,

            @RequestParam("contactNumber")
            String contactNumber,

            @RequestParam("bio")
            String bio,

            HttpSession session,

            RedirectAttributes redirectAttributes) {


        User user =
                getLoggedInUser(session);


        if (user == null ||
                !isAdmin(user)) {

            return "redirect:/login";
        }


        // -------------------------------------------------
        // VALIDATE ACCOUNT DETAILS
        // -------------------------------------------------

        if (fullName == null ||
                fullName.trim().isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Full name cannot be empty."
            );


            return "redirect:/admin/profile/edit";
        }


        // -------------------------------------------------
        // UPDATE USER TABLE
        // -------------------------------------------------

        user.setFullName(
                fullName.trim()
        );


        user.setPhone(
                phone == null
                        ? ""
                        : phone.trim()
        );


        userRepository.save(user);


        // -------------------------------------------------
        // FIND ARTISAN PROFILE
        // -------------------------------------------------

        Artisan artisan =
                artisanRepository
                        .findByUserUserId(
                                user.getUserId()
                        )
                        .orElse(null);


        // -------------------------------------------------
        // UPDATE ARTISAN TABLE
        // -------------------------------------------------

        if (artisan != null) {

            artisan.setCraftType(
                    craftType == null
                            ? ""
                            : craftType.trim()
            );


            artisan.setLocation(
                    location == null
                            ? ""
                            : location.trim()
            );


            artisan.setContactNumber(
                    contactNumber == null
                            ? ""
                            : contactNumber.trim()
            );


            artisan.setBio(
                    bio == null
                            ? ""
                            : bio.trim()
            );


            artisanRepository.save(
                    artisan
            );
        }


        // -------------------------------------------------
        // UPDATE SESSION USER
        // -------------------------------------------------

        session.setAttribute(
                "loggedInUser",
                user
        );


        // -------------------------------------------------
        // SUCCESS MESSAGE
        // -------------------------------------------------

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Profile updated successfully."
        );


        return "redirect:/admin/profile";
    }

    

}