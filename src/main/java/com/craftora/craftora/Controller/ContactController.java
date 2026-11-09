
package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Entity.ContactMessage;
import com.craftora.craftora.Repository.ArtisanRepository;
import com.craftora.craftora.Repository.ContactMessageRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ContactController {

    private final ArtisanRepository artisanRepository;
    private final ContactMessageRepository contactMessageRepository;

    public ContactController(
            ArtisanRepository artisanRepository,
            ContactMessageRepository contactMessageRepository) {

        this.artisanRepository = artisanRepository;
        this.contactMessageRepository = contactMessageRepository;
    }

    @GetMapping("/contact")
    public String showContactPage(Model model) {

        List<Artisan> approvedArtisans =
                artisanRepository.findByStatusIgnoreCase("APPROVED");

        List<Artisan> mapArtisans = approvedArtisans.stream()
                .filter(artisan ->
                        artisan.getLatitude() != null &&
                        artisan.getLongitude() != null &&
                        artisan.getLatitude() >= -90 &&
                        artisan.getLatitude() <= 90 &&
                        artisan.getLongitude() >= -180 &&
                        artisan.getLongitude() <= 180
                )
                .collect(Collectors.toList());

        model.addAttribute("artisans", mapArtisans);

        return "contact";
    }

    @PostMapping("/contact/send")
    public String submitContactForm(
            @ModelAttribute ContactMessage contactMessage,
            RedirectAttributes redirectAttributes) {

        contactMessage.setStatus("NEW");

        contactMessageRepository.save(contactMessage);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Your message has been sent successfully!"
        );

        return "redirect:/contact";
    }
}