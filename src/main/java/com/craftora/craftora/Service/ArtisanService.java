package com.craftora.craftora.Service;

import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Entity.ArtisanCertificate;
import com.craftora.craftora.Entity.ArtisanRequest;
import com.craftora.craftora.Entity.User;

import com.craftora.craftora.Repository.ArtisanCertificateRepository;
import com.craftora.craftora.Repository.ArtisanRepository;
import com.craftora.craftora.Repository.ArtisanRequestRepository;
import com.craftora.craftora.Repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Service
public class ArtisanService {

    private final UserRepository userRepository;
    private final ArtisanRepository artisanRepository;
    private final ArtisanRequestRepository artisanRequestRepository;
    private final ArtisanCertificateRepository artisanCertificateRepository;
    private final EmailService emailService;

    public ArtisanService(
            UserRepository userRepository,
            ArtisanRepository artisanRepository,
            ArtisanRequestRepository artisanRequestRepository,
            ArtisanCertificateRepository artisanCertificateRepository,
            EmailService emailService) {

        this.userRepository = userRepository;
        this.artisanRepository = artisanRepository;
        this.artisanRequestRepository = artisanRequestRepository;
        this.artisanCertificateRepository = artisanCertificateRepository;
        this.emailService = emailService;
    }


    // =====================================================
    // ARTISAN REGISTRATION
    // =====================================================

    public void registerArtisan(
            String firstName,
            String lastName,
            String email,
            String phone,
            String craftType,
            String location,
            String description,
            String password,
            MultipartFile[] certificates) throws IOException {

        // =====================================================
        // REMOVE UNNECESSARY SPACES
        // =====================================================

        firstName = firstName.trim();
        lastName = lastName.trim();
        email = email.trim();
        phone = phone.trim();
        craftType = craftType.trim();
        location = location.trim();
        description = description.trim();


        // =====================================================
        // VALIDATE CERTIFICATES
        // =====================================================

        if (certificates == null || certificates.length < 1) {

            throw new RuntimeException(
                    "At least one certificate is required."
            );
        }

        if (certificates.length > 4) {

            throw new RuntimeException(
                    "Maximum 4 certificates are allowed."
            );
        }


        // =====================================================
        // FIND EXISTING USER BY EMAIL
        // =====================================================

        User user =
                userRepository.findByEmail(email).orElse(null);


        // =====================================================
        // EXISTING USER
        // =====================================================

        if (user != null) {

            // -------------------------------------------------
            // CHECK WHETHER USER ALREADY HAS ARTISAN ROLE
            // -------------------------------------------------

            boolean alreadyArtisan =
                    user.getRoles()
                            .stream()
                            .anyMatch(role ->
                                    role.getRoleName()
                                            .equalsIgnoreCase("ARTISAN")
                            );

            // -------------------------------------------------
            // SAME EMAIL CANNOT REGISTER AS ARTISAN TWICE
            // -------------------------------------------------

            if (alreadyArtisan) {

                throw new RuntimeException(
                        "This email is already registered as an Artisan."
                );
            }


            // -------------------------------------------------
            // CHECK EXISTING PENDING / APPROVED APPLICATION
            // -------------------------------------------------

            boolean existingApplication =
                    artisanRequestRepository
                            .findByUserId(user.getUserId())
                            .stream()
                            .anyMatch(request -> {

                                String status =
                                        request.getStatus();

                                return status != null &&
                                        (
                                                status.equalsIgnoreCase("PENDING")
                                                        ||
                                                status.equalsIgnoreCase("APPROVED")
                                        );
                            });

            if (existingApplication) {

                throw new RuntimeException(
                        "You already have an Artisan application."
                );
            }

            /*
             * Existing Customer can apply
             * as Artisan.
             *
             * The same User account is reused.
             *
             * The ARTISAN role will be added
             * after Admin approval.
             */
        }


        // =====================================================
        // CREATE NEW USER IF USER DOES NOT EXIST
        // =====================================================

        if (user == null) {

            user = new User();

            user.setFullName(
                    firstName + " " + lastName
            );

            user.setEmail(email);

            user.setPhone(phone);

            // -------------------------------------------------
            // ENCRYPT PASSWORD USING BCRYPT
            // -------------------------------------------------

            user.setPassword(
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                            .encode(password)
            );

            user.setCreatedAt(
                    LocalDateTime.now()
            );

            user =
                    userRepository.save(user);
        }


        // =====================================================
        // FIND EXISTING ARTISAN RECORD
        // =====================================================

        Artisan artisan =
                artisanRepository
                        .findByUserUserId(user.getUserId())
                        .orElse(null);

        /*
         * If a previous application was rejected,
         * reuse the existing Artisan record.
         */

        if (artisan == null) {

            artisan = new Artisan();

            artisan.setUser(user);

            artisan.setCreatedAt(
                    LocalDateTime.now()
            );

        } else {

            // Reuse existing Artisan record
            artisan.setUser(user);
        }


        // =====================================================
        // SET ARTISAN INFORMATION
        // =====================================================

        artisan.setCraftType(craftType);

        artisan.setLocation(location);

        artisan.setBio(description);

        artisan.setContactNumber(phone);


        // =====================================================
        // NEW ARTISAN APPLICATION STARTS AS PENDING
        // =====================================================

        artisan.setStatus("PENDING");

        artisanRepository.save(artisan);


        // =====================================================
        // CREATE ARTISAN REQUEST
        // =====================================================

        ArtisanRequest request =
                new ArtisanRequest();

        request.setUserId(
                user.getUserId()
        );

        request.setFirstName(
                firstName
        );

        request.setLastName(
                lastName
        );

        request.setEmail(
                email
        );

        request.setContactNumber(
                phone
        );


        // =====================================================
        // SAVE REGISTRATION FEE AT REQUEST TIME
        // =====================================================

        /*
         * The current Artisan Registration Fee is
         * LKR 2,500.00.
         *
         * This amount is saved with the request.
         *
         * If the Admin changes the fee later,
         * this existing request will still keep
         * its original fee.
         */

        request.setRegistrationFee(
                new BigDecimal("2500.00")
        );


        // =====================================================
        // REQUEST STATUS
        // =====================================================

        request.setStatus(
                "PENDING"
        );

        request.setRequestedAt(
                LocalDateTime.now()
        );


        // =====================================================
        // SAVE ARTISAN REQUEST
        // =====================================================

        ArtisanRequest savedRequest =
                artisanRequestRepository.save(request);


        // =====================================================
        // CREATE CERTIFICATE UPLOAD FOLDER
        // =====================================================

        Path uploadDirectory =
                Paths.get(
                        "uploads/artisan-certificates"
                );

        Files.createDirectories(
                uploadDirectory
        );


        // =====================================================
        // SAVE CERTIFICATES
        // =====================================================

        for (MultipartFile certificate :
                certificates) {

            if (certificate == null ||
                    certificate.isEmpty()) {

                continue;
            }


            String originalFileName =
                    certificate.getOriginalFilename();

            if (originalFileName == null ||
                    originalFileName.isBlank()) {

                continue;
            }


            // -------------------------------------------------
            // CREATE UNIQUE FILE NAME
            // -------------------------------------------------

            String safeFileName =
                    System.currentTimeMillis()
                            + "_"
                            + originalFileName;


            Path filePath =
                    uploadDirectory.resolve(
                            safeFileName
                    );


            // -------------------------------------------------
            // SAVE PHYSICAL FILE
            // -------------------------------------------------

            Files.copy(
                    certificate.getInputStream(),
                    filePath
            );


            // -------------------------------------------------
            // SAVE CERTIFICATE INFORMATION
            // -------------------------------------------------

            ArtisanCertificate artisanCertificate =
                    new ArtisanCertificate();

            artisanCertificate.setRequestId(
                    savedRequest.getRequestId()
            );

            artisanCertificate.setFileName(
                    originalFileName
            );

            artisanCertificate.setFilePath(
                    filePath.toString()
            );

            artisanCertificate.setUploadedAt(
                    LocalDateTime.now()
            );


            artisanCertificateRepository.save(
                    artisanCertificate
            );
        }


        // =====================================================
        // SEND ARTISAN SUBMISSION EMAIL
        // =====================================================

        /*
         * Email failure should NOT cancel
         * the database registration.
         */

        try {

            emailService.sendArtisanSubmissionEmail(
                    email,
                    firstName + " " + lastName
            );

        } catch (Exception e) {

            System.out.println(
                    "Artisan submission email could not be sent: "
                            + e.getMessage()
            );
        }
    }
}