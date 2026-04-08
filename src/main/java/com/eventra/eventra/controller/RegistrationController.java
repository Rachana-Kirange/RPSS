package com.eventra.eventra.controller;

import com.eventra.eventra.model.User;
import com.eventra.eventra.model.Registration;
import com.eventra.eventra.model.Role;
import com.eventra.eventra.service.RegistrationService;
import com.eventra.eventra.service.EventService;
import com.eventra.eventra.service.PassService;

import jakarta.servlet.http.HttpSession;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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

    public RegistrationController(RegistrationService registrationService,
                                  EventService eventService,
                                  PassService passService) {
        this.registrationService = registrationService;
        this.eventService = eventService;
        this.passService = passService;
    }

    // ===================== COMMON METHOD =====================
    private User getLoggedInUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }

    // ===================== REGISTER =====================
    @PostMapping("/register/{eventId}")
    public String registerForEvent(@PathVariable Long eventId,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {

        User user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/auth/login";
        }

        // ROLE CHECK (IMPORTANT)
        if (!user.getRole().equals(Role.PARTICIPANT)) {
            redirectAttributes.addFlashAttribute("error", "Only participants can register for events");
            return "redirect:/dashboard";
        }

        try {
            Registration registration = registrationService.registerForEvent(eventId, user);

            var event = eventService.getEventById(eventId);

            if (event.isPresent() && event.get().getRequiresPayment()) {
                return "redirect:/payments/initiate/" + registration.getRegistrationId();
            } else {
                passService.generateQRPass(registration);

                log.info(() -> "User registered for event: " + eventId +
                        " Registration ID: " + registration.getRegistrationId());

                return "redirect:/registrations/" + registration.getRegistrationId() + "/pass";
            }

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
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