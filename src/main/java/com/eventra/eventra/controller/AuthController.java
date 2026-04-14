package com.eventra.eventra.controller;

import com.eventra.eventra.model.User;
import com.eventra.eventra.dto.UserLoginDTO;
import com.eventra.eventra.dto.UserRegistrationDTO;
import com.eventra.eventra.enums.RoleEnum;
import com.eventra.eventra.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.logging.Logger;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = Logger.getLogger(AuthController.class.getName());

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Display login page
     */
    @GetMapping("/login")
    public String showLoginPage(Model model) {
        model.addAttribute("userLoginDTO", new UserLoginDTO());
        return "auth/login";
    }

    /**
     * Handle login
     */
    @PostMapping("/login")
    public String handleLogin(@Valid @ModelAttribute UserLoginDTO loginDTO,
                             BindingResult result, HttpSession session, Model model) {
        try {
            if (result.hasErrors()) {
                return "auth/login";
            }

            log.info("Attempting to authenticate user: " + loginDTO.getEmail());
            Optional<User> user = userService.authenticateUser(loginDTO);

            if (user.isPresent()) {
                User authenticatedUser = user.get();
                log.info("User found: " + authenticatedUser.getEmail() + ", Role: " + authenticatedUser.getRole());
                
                // Verify user has a role assigned
                if (authenticatedUser.getRole() == null) {
                    model.addAttribute("error", "Your account has not been assigned a role. Please contact the administrator.");
                    return "auth/login";
                }
                
                session.setAttribute("loggedInUser", authenticatedUser);
                session.setAttribute("userId", authenticatedUser.getUserId());
                session.setAttribute("userRole", authenticatedUser.getRole().getRoleName());
                log.info(String.format("User logged in: %s with role: %s", loginDTO.getEmail(), authenticatedUser.getRole().getRoleName()));
                return "redirect:/dashboard";
            } else {
                model.addAttribute("error", "Invalid email or password");
                return "auth/login";
            }
        } catch (Exception e) {
            log.severe("Error during login: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "An error occurred during login: " + e.getMessage());
            return "auth/login";
        }
    }

    /**
     * Display registration page
     */
    @GetMapping("/register")
    public String showRegistrationPage(Model model) {
        model.addAttribute("userRegistrationDTO", new UserRegistrationDTO());
        model.addAttribute("roles", RoleEnum.values());
        return "auth/register";
    }

    /**
     * Handle registration
     */
    @PostMapping("/register")
    public String handleRegistration(@Valid @ModelAttribute UserRegistrationDTO registrationDTO,
                                    BindingResult result, Model model) {
        try {
            log.info(String.format("Registration attempt: email=%s, name=%s, role=%s", 
                registrationDTO.getEmail(), registrationDTO.getName(), registrationDTO.getRole()));
            
            if (result.hasErrors()) {
                log.warning("Validation errors in registration: " + result.getAllErrors());
                model.addAttribute("roles", RoleEnum.values());
                return "auth/register";
            }

            // Validate password confirmation
            if (!registrationDTO.getPassword().equals(registrationDTO.getConfirmPassword())) {
                result.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
                model.addAttribute("roles", RoleEnum.values());
                return "auth/register";
            }

            User savedUser = userService.registerUser(registrationDTO);
            log.info(String.format("User registered successfully: %s (ID: %d) with role: %s", 
                savedUser.getEmail(), savedUser.getUserId(), savedUser.getRole().getRoleName()));
            model.addAttribute("success", "Registration successful! Please login.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            log.warning("Registration failed: " + e.getMessage());
            // Handle both email and phone duplicate errors
            if (e.getMessage().contains("Email already exists")) {
                result.rejectValue("email", "email.exists", e.getMessage());
            } else if (e.getMessage().contains("Phone number already registered")) {
                result.rejectValue("phone", "phone.exists", e.getMessage());
            } else if (e.getMessage().contains("role is not available")) {
                result.rejectValue("role", "error", e.getMessage());
            } else {
                result.rejectValue("email", "error", e.getMessage());
            }
            model.addAttribute("roles", RoleEnum.values());
            return "auth/register";
        } catch (Exception e) {
            log.severe("Error during registration: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "An error occurred during registration: " + e.getMessage());
            model.addAttribute("roles", RoleEnum.values());
            return "auth/register";
        }
    }

    /**
     * Logout user
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        log.info("User logged out");
        return "redirect:/auth/login";
    }

    /**
     * Check session
     */
    @GetMapping("/check-session")
    public String checkSession(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null) {
            return "redirect:/dashboard";
        }
        return "redirect:/auth/login";
    }
}
