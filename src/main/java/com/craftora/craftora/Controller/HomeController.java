package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Repository.ArtisanRepository;
import com.craftora.craftora.Service.ArtisanService;
import com.craftora.craftora.Service.UserService;
import com.craftora.craftora.Entity.Event;
import com.craftora.craftora.Repository.EventRepository;
import com.craftora.craftora.Repository.EventRegistrationRepository;


import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class HomeController {

private final ArtisanRepository artisanRepository;
private final UserService userService;
private final ArtisanService artisanService;
private final EventRepository eventRepository;
private final EventRegistrationRepository eventRegistrationRepository;

  public HomeController(
        ArtisanRepository artisanRepository,
        UserService userService,
        ArtisanService artisanService,
        EventRepository eventRepository,
        EventRegistrationRepository eventRegistrationRepository) {
        
    this.artisanRepository = artisanRepository;
    this.userService = userService;
    this.artisanService = artisanService;
    this.eventRepository = eventRepository;
    this.eventRegistrationRepository = eventRegistrationRepository;
}

    // ==========================================
    // HOME PAGE
    // ==========================================


@GetMapping("/")
public String home(Model model) {

    List<Artisan> artisans =
            artisanRepository.findByStatusIgnoreCase("APPROVED");

    // Display only 4 approved artisans on the home page
    List<Artisan> homeArtisans = artisans.stream()
            .limit(4)
            .toList();

    List<Event> events =
            eventRepository.findByStatusIgnoreCase("PUBLISHED");

    model.addAttribute("artisans", homeArtisans);

    model.addAttribute("events", events);

    return "home";
}


    // ==========================================
    // ABOUT US PAGE
    // ==========================================

    @GetMapping("/about-us")
    public String aboutUs() {

        return "about-us";
    }

    // ==========================================
    // EVENTS - WORKSHOPS PAGE
    // ==========================================

   @GetMapping("/workshops")
public String workshops(Model model) {

    List<Event> workshops =
            eventRepository
                    .findByStatusIgnoreCaseAndEventTypeIgnoreCase(
                            "PUBLISHED",
                            "WORKSHOP"
                    );

    model.addAttribute(
            "events",
            workshops
    );

    return "workshops";
}
    // ==========================================
    // EVENTS - EXHIBITIONS PAGE
    // ==========================================

@GetMapping({"/exhibitions", "/Exhibitions"})
public String exhibitions(Model model) {

    List<Event> exhibitions =
            eventRepository
                    .findByStatusIgnoreCaseAndEventTypeIgnoreCase(
                            "PUBLISHED",
                            "EXHIBITION"
                    );

    model.addAttribute(
            "events",
            exhibitions
    );

    return "exhibitions";
}

 // ==========================================
 // EVENTS - COMPETITIONS PAGE
 // ==========================================

@GetMapping({"/competitions", "/Competitions"})
public String competitions(Model model) {

    List<Event> competitions =
            eventRepository
                    .findByStatusIgnoreCaseAndEventTypeIgnoreCase(
                            "PUBLISHED",
                            "COMPETITION"
                    )
                    .stream()
                    .filter(event ->
                            Boolean.TRUE.equals(
                                    event.getAllowCustomers()
                            )
                    )
                    .toList();

    model.addAttribute("events", competitions);

    return "competitions";
}


// ==========================================
// EVENT DETAILS PAGE
// ==========================================

@GetMapping("/events/{id}")
public String eventDetails(
        @PathVariable Long id,
        Model model,
HttpSession session) {

    Event event = eventRepository
            .findById(id)
            .orElseThrow(() ->
                    new RuntimeException("Event not found"));


                    
if ("COMPETITION".equalsIgnoreCase(event.getEventType())) {

    // Redirect guests to login.
    if (session.getAttribute("loggedInUserId") == null) {
        return "redirect:/login";
    }

    // Customers cannot view artisan-only competitions.
    Object selectedRole = session.getAttribute("selectedRole");

    if ("CUSTOMER".equalsIgnoreCase(
            selectedRole == null ? "" : selectedRole.toString())
            && !Boolean.TRUE.equals(event.getAllowCustomers())) {

        return "redirect:/competitions";
    }
}



    // ==========================================
    // CALCULATE AVAILABLE SEATS
    // ==========================================

    Long availableSeats = null;

    // If admin has entered a capacity,
    // calculate the remaining seats.
    if (event.getCapacity() != null
            && event.getCapacity() > 0) {

        long confirmedCount =
                eventRegistrationRepository
                        .countByEventIdAndRegistrationStatusIgnoreCase(
                                event.getEventId(),
                                "CONFIRMED"
                        );

        availableSeats =
                Math.max(
                        0L,
                        event.getCapacity() - confirmedCount
                );
    }


    // ==========================================
    // SEND DATA TO HTML
    // ==========================================

    model.addAttribute(
            "event",
            event
    );

    model.addAttribute(
            "availableSeats",
            availableSeats
    );


    return "event-details";
}


    // ==========================================
    // REGISTER PAGE
    // ==========================================

    @GetMapping("/register")
    public String register(HttpServletResponse response) {

        /*
         * Prevent the browser from restoring an old
         * registration page from cache/history.
         */

        response.setHeader(
                "Cache-Control",
                "no-store, no-cache, must-revalidate, max-age=0"
        );

        response.setHeader(
                "Pragma",
                "no-cache"
        );

        response.setDateHeader(
                "Expires",
                0
        );

        return "register";
    }

    // ==========================================
    // CUSTOMER REGISTRATION
    // ==========================================

    @PostMapping("/register")
    public String registerCustomer(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String password,
            Model model) {

        try {

            userService.registerCustomer(
                    firstName,
                    lastName,
                    email,
                    phone,
                    password
            );

            /*
             * Registration successful.
             * Redirect to login page.
             */

            return "redirect:/login";

        } catch (RuntimeException e) {

            String message = e.getMessage();

            // ==========================================
            // DUPLICATE CUSTOMER
            // ==========================================

            if (message != null &&
                    message.startsWith(
                            "This email is already registered as a Customer."
                    )) {

                model.addAttribute(
                        "errorMessage",
                        "This email is already registered as a Customer. "
                                + "You can only have one Customer account per email."
                );
            }

            // ==========================================
            // CUSTOMER ROLE NOT FOUND
            // ==========================================

            else if ("CUSTOMER role was not found."
                    .equals(message)) {

                model.addAttribute(
                        "errorMessage",
                        "Customer role is not configured. "
                                + "Please contact the administrator."
                );
            }

            // ==========================================
            // OTHER CUSTOMER REGISTRATION ERROR
            // ==========================================

            else {

                model.addAttribute(
                        "errorMessage",
                        "Customer registration failed. "
                                + "Please try again."
                );
            }

            return "register";
        }
    }

    // ==========================================
    // ARTISAN REGISTRATION
    // ==========================================

    @PostMapping("/register-artisan")
    public String registerArtisan(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String craftType,
            @RequestParam String location,
            @RequestParam String description,
            @RequestParam String password,
            @RequestParam("certificates")
            MultipartFile[] certificates,
            Model model) {

        try {

            artisanService.registerArtisan(
                    firstName,
                    lastName,
                    email,
                    phone,
                    craftType,
                    location,
                    description,
                    password,
                    certificates
            );

            /*
             * Artisan application submitted successfully.
             * Go to login page.
             */

            return "redirect:/login";

        } catch (RuntimeException e) {

            String message = e.getMessage();

            // ==========================================
            // DUPLICATE ARTISAN
            // ==========================================

            if (message != null &&
                    message.startsWith(
                            "This email is already registered as an Artisan."
                    )) {

                model.addAttribute(
                        "errorMessage",
                        "This email is already registered as an Artisan. "
                                + "You can only have one Artisan account per email."
                );
            }

            // ==========================================
            // EXISTING ARTISAN APPLICATION
            // ==========================================

            else if ("You already have an Artisan application."
                    .equals(message)) {

                model.addAttribute(
                        "errorMessage",
                        "You already have an Artisan application for this email. "
                                + "Please wait for the admin to review your application."
                );
            }

            // ==========================================
            // NO CERTIFICATE
            // ==========================================

            else if ("At least one certificate is required."
                    .equals(message)) {

                model.addAttribute(
                        "errorMessage",
                        "Please upload at least one certificate."
                );
            }

            // ==========================================
            // TOO MANY CERTIFICATES
            // ==========================================

            else if ("Maximum 4 certificates are allowed."
                    .equals(message)) {

                model.addAttribute(
                        "errorMessage",
                        "You can upload a maximum of 4 certificates."
                );
            }

            // ==========================================
            // OTHER ARTISAN REGISTRATION ERROR
            // ==========================================

            else {

                model.addAttribute(
                        "errorMessage",
                        "Artisan registration failed. "
                                + "Please try again."
                );
            }

            return "register";

        } catch (Exception e) {

            e.printStackTrace();

            model.addAttribute(
                    "errorMessage",
                    "Registration failed. Please try again."
            );

            return "register";
        }
    }
}
