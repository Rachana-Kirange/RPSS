package com.eventra.eventra.controller;

import com.eventra.eventra.model.Registration;
import com.eventra.eventra.model.User;
import com.eventra.eventra.service.PassService;
import com.eventra.eventra.service.RegistrationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    private final RegistrationService registrationService;
    private final PassService passService;

    public PaymentController(RegistrationService registrationService, PassService passService) {
        this.registrationService = registrationService;
        this.passService = passService;
    }

    @GetMapping("/initiate/{registrationId}")
    public String showPaymentPage(@PathVariable Long registrationId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/auth/login";
        }

        Registration registration = registrationService.getRegistrationById(registrationId)
            .orElseThrow(() -> new RuntimeException("Registration not found"));

        if (!registration.getStudent().getUserId().equals(user.getUserId())) {
            return "error/unauthorized";
        }

        if (!registration.isPaymentRequired()) {
            return "redirect:/registrations/" + registrationId + "/pass";
        }

        model.addAttribute("registration", registration);
        model.addAttribute("event", registration.getEvent());
        return "payment/initiate";
    }

    @PostMapping("/initiate/{registrationId}")
    public String completePayment(@PathVariable Long registrationId,
                                  @RequestParam @NotBlank String transactionId,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/auth/login";
        }

        Registration registration = registrationService.getRegistrationById(registrationId)
            .orElseThrow(() -> new RuntimeException("Registration not found"));

        if (!registration.getStudent().getUserId().equals(user.getUserId())) {
            return "redirect:/dashboard";
        }

        registrationService.completePayment(registrationId, transactionId);
        passService.getPassByRegistration(registrationId)
            .orElseGet(() -> passService.generateQRPass(registration));

        redirectAttributes.addFlashAttribute("success", "Payment completed successfully.");
        return "redirect:/registrations/" + registrationId + "/pass";
    }
}
