
package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.PreviousEvent;
import com.craftora.craftora.Repository.PreviousEventRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/admin/previous-events")
public class AdminPreviousEventController {

    private final PreviousEventRepository previousEventRepository;

    public AdminPreviousEventController(
            PreviousEventRepository previousEventRepository) {
        this.previousEventRepository = previousEventRepository;
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("previousEvent", new PreviousEvent());
        return "admin-create-previous-event";
    }

    @PostMapping("/save")
    public String savePreviousEvent(
            @ModelAttribute("previousEvent") PreviousEvent previousEvent,
            @RequestParam("eventImageFile") MultipartFile eventImageFile,
            RedirectAttributes redirectAttributes,
            Model model) {

        String type = previousEvent.getEventType();

        if (type == null ||
                !(type.equals("WORKSHOP")
                || type.equals("EXHIBITION")
                || type.equals("COMPETITION"))) {

            model.addAttribute("errorMessage",
                    "Please select a valid event type.");
            return "admin-create-previous-event";
        }

        if (previousEvent.getEventTitle() == null ||
                previousEvent.getEventTitle().trim().isEmpty()) {

            model.addAttribute("errorMessage",
                    "Event title is required.");
            return "admin-create-previous-event";
        }

        if (previousEvent.getEventDate() == null ||
                !previousEvent.getEventDate().isBefore(LocalDate.now())) {

            model.addAttribute("errorMessage",
                    "Please select a date before today for a previous event.");
            return "admin-create-previous-event";
        }

        if (!eventImageFile.isEmpty() &&
                (eventImageFile.getContentType() == null ||
                !eventImageFile.getContentType().startsWith("image/"))) {

            model.addAttribute("errorMessage",
                    "Please upload a valid image file.");
            return "admin-create-previous-event";
        }

        try {
            if (!eventImageFile.isEmpty()) {

                String originalName = eventImageFile.getOriginalFilename();
                String extension = "";

                if (originalName != null && originalName.contains(".")) {
                    extension = originalName.substring(
                            originalName.lastIndexOf("."));
                }

                String fileName = UUID.randomUUID() + extension;

                Path uploadPath = Paths.get("uploads/previous-events");
                Files.createDirectories(uploadPath);

                Path filePath = uploadPath.resolve(fileName).normalize();

                if (!filePath.startsWith(uploadPath.toAbsolutePath().normalize())
                        && filePath.isAbsolute()) {
                    throw new IOException("Invalid upload path.");
                }

                Files.copy(eventImageFile.getInputStream(),
                        uploadPath.resolve(fileName),
                        StandardCopyOption.REPLACE_EXISTING);

                previousEvent.setEventImage(
                        "/uploads/previous-events/" + fileName);
            }

            previousEvent.setEventTitle(
                    previousEvent.getEventTitle().trim());

            previousEventRepository.save(previousEvent);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Previous event added successfully!");

            return "redirect:/admin/previous-events/create";

        } catch (IOException e) {
            model.addAttribute("errorMessage",
                    "Image upload failed. Please try again.");
            return "admin-create-previous-event";
        }
    }
}
