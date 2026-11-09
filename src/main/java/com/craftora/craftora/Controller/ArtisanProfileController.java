package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Entity.ArtisanRequest;
import com.craftora.craftora.Entity.User;
import com.craftora.craftora.Repository.ArtisanRepository;
import com.craftora.craftora.Repository.ArtisanRequestRepository;
import com.craftora.craftora.Repository.UserRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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

@Controller
public class ArtisanProfileController {

    private final ArtisanRepository artisanRepository;
    private final ArtisanRequestRepository artisanRequestRepository;
    private final UserRepository userRepository;

    public ArtisanProfileController(
            ArtisanRepository artisanRepository,
            ArtisanRequestRepository artisanRequestRepository,
            UserRepository userRepository) {

        this.artisanRepository = artisanRepository;
        this.artisanRequestRepository = artisanRequestRepository;
        this.userRepository = userRepository;
    }


    // =========================================================
    // SHOW ARTISAN PROFILE
    // =========================================================

    @GetMapping("/artisan/profile")
    public String showArtisanProfile(
            HttpSession session,
            Model model) {

        Object loggedInUserId =
                session.getAttribute("loggedInUserId");

        // User is not logged in
        if (loggedInUserId == null) {
            return "redirect:/login";
        }

        Long userId =
                Long.valueOf(loggedInUserId.toString());


        // Get logged-in user
        User user =
                userRepository.findById(userId).orElse(null);

        if (user == null) {
            return "redirect:/login";
        }


        // Get artisan profile
        Artisan artisan =
                artisanRepository.findByUserUserId(userId)
                        .orElse(null);


        // Get artisan requests
        List<ArtisanRequest> requests =
                artisanRequestRepository.findByUserId(userId);


        // Find approved request
        ArtisanRequest approvedRequest = null;

        for (ArtisanRequest request : requests) {

            if ("APPROVED".equalsIgnoreCase(
                    request.getStatus())) {

                approvedRequest = request;
                break;
            }
        }


        model.addAttribute("user", user);

        model.addAttribute(
                "artisan",
                artisan
        );

        model.addAttribute(
                "artisanRequest",
                approvedRequest
        );


        return "artisan-profile";
    }


    // =========================================================
    // SAVE / UPDATE ARTISAN PROFILE
    // =========================================================

    @PostMapping("/artisan/profile/save")
    public String saveArtisanProfile(

            HttpSession session,

            @RequestParam("craftType")
            String craftType,

            @RequestParam(value = "craftSpecialization",
                    required = false)
            String craftSpecialization,

            @RequestParam(value = "yearsOfExperience",
                    required = false)
            Integer yearsOfExperience,

            @RequestParam(value = "bio",
                    required = false)
            String bio,

            @RequestParam(value = "contactNumber",
                    required = false)
            String contactNumber,

            @RequestParam(value = "location",
                    required = false)
            String location,

            @RequestParam(value = "latitude",
                    required = false)
            Double latitude,

            @RequestParam(value = "longitude",
                    required = false)
            Double longitude,

            @RequestParam(value = "studioName",
                    required = false)
            String studioName,

            @RequestParam(value = "availability",
                    required = false)
            String availability,

            @RequestParam(value = "facebook",
                    required = false)
            String facebook,

            @RequestParam(value = "instagram",
                    required = false)
            String instagram,

            @RequestParam(value = "website",
                    required = false)
            String website,

            @RequestParam(value = "profileImage",
                    required = false)
            MultipartFile profileImage,

            RedirectAttributes redirectAttributes) {


        // =====================================================
        // CHECK LOGIN
        // =====================================================

        Object loggedInUserId =
                session.getAttribute("loggedInUserId");

        if (loggedInUserId == null) {
            return "redirect:/login";
        }


        Long userId =
                Long.valueOf(loggedInUserId.toString());


        // =====================================================
        // GET USER
        // =====================================================

        User user =
                userRepository.findById(userId).orElse(null);

        if (user == null) {
            return "redirect:/login";
        }


        // =====================================================
        // GET OR CREATE ARTISAN
        // =====================================================

        Artisan artisan =
                artisanRepository.findByUserUserId(userId)
                        .orElse(null);


        if (artisan == null) {

            artisan = new Artisan();

            artisan.setUser(user);

            artisan.setCreatedAt(
                    LocalDateTime.now()
            );

            artisan.setStatus("ACTIVE");
        }


        // =====================================================
        // SET PROFILE DATA
        // =====================================================

        artisan.setCraftType(craftType);

        artisan.setCraftSpecialization(
                craftSpecialization
        );

        artisan.setYearsOfExperience(
                yearsOfExperience
        );

        artisan.setBio(bio);

        artisan.setContactNumber(
                contactNumber
        );

        artisan.setLocation(
                location
        );

        artisan.setLatitude(
                latitude
        );

        artisan.setLongitude(
                longitude
        );

        artisan.setStudioName(
                studioName
        );

        artisan.setAvailability(
                availability
        );

        artisan.setFacebook(
                facebook
        );

        artisan.setInstagram(
                instagram
        );

        artisan.setWebsite(
                website
        );


        // =====================================================
        // PROFILE IMAGE UPLOAD
        // =====================================================

        /*
         * Keep the old image path.
         *
         * When a new image is selected:
         *
         * Old image
         *      ↓
         * New image uploaded
         *      ↓
         * Database updated with new image
         *      ↓
         * Old image file deleted
         *
         * Therefore only the current profile picture
         * remains in the uploads folder.
         */

        String oldProfileImage =
                artisan.getProfileImage();


        if (profileImage != null &&
                !profileImage.isEmpty()) {

            try {

                // Upload directory
                String uploadDirectory =
                        "uploads/artisans/";


                // Create upload folder if it does not exist
                Path uploadPath =
                        Paths.get(uploadDirectory);

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }


                // Get original file name
                String originalFileName =
                        profileImage.getOriginalFilename();


                // Get file extension
                String extension = "";

                if (originalFileName != null &&
                        originalFileName.contains(".")) {

                    extension =
                            originalFileName.substring(
                                    originalFileName.lastIndexOf(".")
                            );
                }


                // Create unique file name
                String fileName =
                        "artisan_" +
                        userId +
                        "_" +
                        System.currentTimeMillis() +
                        extension;


                // Create complete file path
                Path filePath =
                        uploadPath.resolve(fileName);


                // Save new image
                Files.copy(
                        profileImage.getInputStream(),
                        filePath
                );


                // Update database with new image path
                artisan.setProfileImage(
                        "/uploads/artisans/" + fileName
                );


            } catch (IOException e) {

                e.printStackTrace();

                redirectAttributes.addFlashAttribute(
                        "error",
                        "Failed to upload profile picture."
                );

                return "redirect:/artisan/profile";
            }
        }


        // =====================================================
        // SAVE ARTISAN
        // =====================================================

        artisanRepository.save(artisan);


        // =====================================================
        // DELETE OLD PROFILE IMAGE
        // =====================================================

        /*
         * Delete the old image only after the new profile
         * information has been successfully saved.
         */

        if (profileImage != null &&
                !profileImage.isEmpty() &&
                oldProfileImage != null &&
                !oldProfileImage.isBlank()) {

            try {

                String oldFileName =
                        oldProfileImage.substring(
                                oldProfileImage.lastIndexOf("/") + 1
                        );


                Path uploadPath =
                        Paths.get("uploads/artisans/");


                Path oldFilePath =
                        uploadPath.resolve(oldFileName);


                Files.deleteIfExists(oldFilePath);


            } catch (Exception e) {

                /*
                 * If old image cannot be deleted,
                 * do not stop the profile update.
                 */

                e.printStackTrace();
            }
        }


        // =====================================================
        // SUCCESS MESSAGE
        // =====================================================

        redirectAttributes.addFlashAttribute(
                "success",
                "Your artisan profile has been saved successfully."
        );


        return "redirect:/artisan/profile";
    }
}