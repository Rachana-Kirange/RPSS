package com.eventra.eventra.controller;

import com.eventra.eventra.model.User;
import com.eventra.eventra.model.Registration;
import com.eventra.eventra.model.Event;
import com.eventra.eventra.dto.EventRegistrationDTO;
import com.eventra.eventra.enums.RoleEnum;
import com.eventra.eventra.service.RegistrationService;
import com.eventra.eventra.service.EventService;
import com.eventra.eventra.service.PassService;
import com.eventra.eventra.service.NotificationService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

import java.util.Optional;
import java.util.logging.Logger;

@Controller
@RequestMapping("/registrations")
public class RegistrationController {

    private static final Logger log = Logger.getLogger(RegistrationController.class.getName());

    private final RegistrationService registrationService;
    private final EventService eventService;
    private final PassService passService;
    private final NotificationService notificationService;

    public RegistrationController(RegistrationService registrationService,
                                  EventService eventService,
                                  PassService passService,
                                  NotificationService notificationService) {
        this.registrationService = registrationService;
        this.eventService = eventService;
        this.passService = passService;
        this.notificationService = notificationService;
    }

    // ===================== COMMON METHOD =====================
    private User getLoggedInUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }

    // ===================== SHOW REGISTRATION FORM =====================
    @GetMapping("/register/{eventId}")
    public String showRegistrationForm(@PathVariable Long eventId,
                                       HttpSession session,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        User user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/auth/login";
        }

        // ROLE CHECK
        if (!user.getRole().getRoleName().equals(RoleEnum.PARTICIPANT)) {
            redirectAttributes.addFlashAttribute("error", "Only participants can register for events");
            return "redirect:/dashboard";
        }

        // Check if event exists
        Optional<Event> event = eventService.getEventById(eventId);
        if (event.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Event not found");
            return "redirect:/dashboard";
        }

        try {
            // Check if already registered
            Optional<Registration> existingReg = registrationService.getByUserAndEvent(user.getUserId(), eventId);
            if (existingReg.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "You are already registered for this event");
                return "redirect:/events/" + eventId;
            }
        } catch (Exception e) {
            log.warning("Error checking existing registration: " + e.getMessage());
        }

        // Pre-fill form with user data
        EventRegistrationDTO dto = new EventRegistrationDTO();
        dto.setEventId(eventId);
        dto.setParticipantFullName(user.getName());
        dto.setParticipantEmail(user.getEmail());
        dto.setMobileNumber(user.getPhone());

        model.addAttribute("eventRegistrationDTO", dto);
        model.addAttribute("event", event.get());

        return "registration/register-form";
    }

    // ===================== PROCESS REGISTRATION =====================
    @PostMapping("/register/{eventId}")
    public String registerForEvent(@PathVariable Long eventId,
                                   @Valid @ModelAttribute EventRegistrationDTO registrationDTO,
                                   BindingResult result,
                                   HttpSession session,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {

        User user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/auth/login";
        }

        // ROLE CHECK
        if (!user.getRole().getRoleName().equals(RoleEnum.PARTICIPANT)) {
            redirectAttributes.addFlashAttribute("error", "Only participants can register for events");
            return "redirect:/dashboard";
        }

        if (result.hasErrors()) {
            Optional<Event> event = eventService.getEventById(eventId);
            model.addAttribute("event", event.orElse(null));
            return "registration/register-form";
        }

        try {
            // Create registration with participant details
            Registration registration = registrationService.registerForEventWithDetails(
                eventId, user, registrationDTO
            );

            // Send notification
            notificationService.sendRegistrationConfirmation(registration);

            // Handle payment flow if required
            Optional<Event> event = eventService.getEventById(eventId);
            if (event.isPresent() && event.get().getRequiresPayment()) {
                return "redirect:/payments/initiate/" + registration.getRegistrationId();
            } else {
                // Generate QR pass
                passService.generateQRPass(registration);
                log.info("User registered for event: " + eventId + 
                        " Registration ID: " + registration.getRegistrationId());
                return "redirect:/registrations/" + registration.getRegistrationId() + "/pass";
            }

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/events/" + eventId;
        } catch (Exception e) {
            log.severe("Error during registration: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "An error occurred: " + e.getMessage());
            return "redirect:/events/" + eventId;
        }
    }

    // ===================== VIEW PASS =====================
    @GetMapping("/{registrationId}/pass")
    public String viewPass(@PathVariable Long registrationId,
                           HttpSession session,
                           Model model) {

        User user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/auth/login";
        }

        Optional<Registration> registration = registrationService.getRegistrationById(registrationId);

        if (registration.isEmpty()) {
            return "error/not-found";
        }

        // OWNERSHIP CHECK
        if (!registration.get().getParticipant().getUserId().equals(user.getUserId())) {
            return "error/unauthorized";
        }

        var pass = passService.getPassByRegistration(registrationId);

        model.addAttribute("registration", registration.get());
        model.addAttribute("pass", pass.orElse(null));

        return "registration/view-pass";
    }

    // ===================== CANCEL =====================
    @PostMapping("/{registrationId}/cancel")
    public String cancelRegistration(@PathVariable Long registrationId,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {

        User user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/auth/login";
        }

        Optional<Registration> registration = registrationService.getRegistrationById(registrationId);

        if (registration.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Registration not found");
            return "redirect:/dashboard";
        }

        if (!registration.get().getParticipant().getUserId().equals(user.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Unauthorized action");
            return "redirect:/dashboard";
        }

        registrationService.cancelRegistration(registrationId);

        log.info(() -> "Registration cancelled: " + registrationId);

        redirectAttributes.addFlashAttribute("success", "Registration cancelled successfully");
        return "redirect:/dashboard";
    }

    // ===================== MY REGISTRATIONS =====================
    @GetMapping("/my-registrations")
    public String viewMyRegistrations(HttpSession session, Model model) {

        User user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/auth/login";
        }

        var registrations = registrationService.getParticipantRegistrations(user.getUserId());
        model.addAttribute("registrations", registrations);

        return "registration/my-registrations";
    }

    // ===================== DOWNLOAD PASS =====================
    @GetMapping("/{registrationId}/download-pass")
    @ResponseBody
    public ResponseEntity<Resource> downloadPass(@PathVariable Long registrationId,
                                                 HttpSession session) {

        User user = getLoggedInUser(session);

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<Registration> registration = registrationService.getRegistrationById(registrationId);

        if (registration.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (!registration.get().getParticipant().getUserId().equals(user.getUserId())) {
            return ResponseEntity.status(403).build();
        }

        Resource file = passService.getPassFile(registrationId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=event-pass.pdf")
                .body(file);
    }
}