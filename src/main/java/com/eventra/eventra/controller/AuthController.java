package com.eventra.eventra.controller;

import com.eventra.eventra.model.User;
import com.eventra.eventra.dto.UserLoginDTO;
import com.eventra.eventra.dto.UserRegistrationDTO;
import com.eventra.eventra.enums.RoleEnum;
import com.eventra.eventra.service.UserService;
import com.eventra.eventra.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Logger;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = Logger.getLogger(AuthController.class.getName());

    private final UserService userService;
    private final AuditLogService auditLogService;

    public AuthController(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Get user agent from request
     */
    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
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
     * Handle login with audit logging
     */
    @PostMapping("/login")
    public String handleLogin(@Valid @ModelAttribute UserLoginDTO loginDTO,
                             BindingResult result, HttpSession session, Model model, 
                             HttpServletRequest request) {
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
                    auditLogService.logFailed(authenticatedUser, "LOGIN", "Login attempted but role not assigned", 
                        "Login failed - no role assigned");
                    return "auth/login";
                }
                
                // Check if user is active
                if (!authenticatedUser.getIsActive()) {
                    model.addAttribute("error", "Your account has been deactivated. Please contact the administrator.");
                    auditLogService.logFailed(authenticatedUser, "LOGIN", "Login attempted on deactivated account", 
                        "Login failed - account inactive");
                    return "auth/login";
                }
                
                session.setAttribute("loggedInUser", authenticatedUser);
                session.setAttribute("userId", authenticatedUser.getUserId());
                session.setAttribute("userRole", authenticatedUser.getRole().getRoleName());
                
                // Create Spring Security authentication
                Collection<GrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority(authenticatedUser.getRole().getRoleName().toString()));
                
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    authenticatedUser.getEmail(),
                    null,
                    authorities
                );
                
                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                securityContext.setAuthentication(authentication);
                SecurityContextHolder.setContext(securityContext);
                session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
                
                // Log successful login
                auditLogService.logLogin(authenticatedUser, getClientIpAddress(request), getUserAgent(request));
                
                log.info(String.format("User logged in: %s with role: %s", loginDTO.getEmail(), authenticatedUser.getRole().getRoleName()));
                return "redirect:/dashboard";
            } else {
                model.addAttribute("error", "Invalid email or password");
                log.warning("Authentication failed for user: " + loginDTO.getEmail());
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
     * Users can register as PARTICIPANT (Student) or CLUB_HEAD
     */
    @GetMapping("/register")
    public String showRegistrationPage(Model model) {
        model.addAttribute("userRegistrationDTO", new UserRegistrationDTO());
        return "auth/register";
    }

    /**
     * Handle registration with role selection
     * - PARTICIPANT: Auto-approved, immediate access to student dashboard
     * - CLUB_HEAD: Pending approval, waiting for admin review
     */
    @PostMapping("/register")
    public String handleRegistration(@Valid @ModelAttribute UserRegistrationDTO registrationDTO,
                                    BindingResult result, Model model) {
        try {
            log.info(String.format("Registration attempt: email=%s, name=%s, role=%s", 
                registrationDTO.getEmail(), registrationDTO.getName(), registrationDTO.getRole()));
            
            if (result.hasErrors()) {
                log.warning("Validation errors in registration: " + result.getAllErrors());
                return "auth/register";
            }

            // Validate that role is selected (only PARTICIPANT or CLUB_HEAD allowed)
            if (registrationDTO.getRole() == null) {
                result.rejectValue("role", "role.required", "Please select a role");
                return "auth/register";
            }

            if (registrationDTO.getRole() != RoleEnum.PARTICIPANT && registrationDTO.getRole() != RoleEnum.CLUB_HEAD) {
                result.rejectValue("role", "role.invalid", "Invalid role selected");
                return "auth/register";
            }

            // Validate password confirmation
            if (!registrationDTO.getPassword().equals(registrationDTO.getConfirmPassword())) {
                result.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
                return "auth/register";
            }

            // Register user with selected role
            User savedUser = userService.registerUser(registrationDTO, registrationDTO.getRole());
            log.info(String.format("User registered successfully: %s (ID: %d) with role: %s, approval status: %s", 
                savedUser.getEmail(), savedUser.getUserId(), registrationDTO.getRole(), savedUser.getApprovalStatus()));
            
            // Redirect to login or approval page depending on role
            if (registrationDTO.getRole() == RoleEnum.CLUB_HEAD) {
                model.addAttribute("success", "Registration successful! Your account is pending admin approval. You will receive a notification once approved. Please login to check your status.");
            } else {
                model.addAttribute("success", "Registration successful! Welcome to Eventra. Please login.");
            }
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            log.warning("Registration failed: " + e.getMessage());
            if (e.getMessage().contains("Email already exists")) {
                result.rejectValue("email", "email.exists", e.getMessage());
            } else if (e.getMessage().contains("Phone number already registered")) {
                result.rejectValue("phone", "phone.exists", e.getMessage());
            } else {
                result.rejectValue("email", "error", e.getMessage());
            }
            return "auth/register";
        } catch (Exception e) {
            log.severe("Error during registration: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "An error occurred during registration: " + e.getMessage());
            return "auth/register";
        }
    }

    /**
     * Logout user with audit logging
     */
    @GetMapping("/logout")
    public String logout(HttpSession session, HttpServletRequest request) {
        User user = (User) session.getAttribute("loggedInUser");
        
        if (user != null) {
            // Log logout
            auditLogService.logLogout(user, getClientIpAddress(request), getUserAgent(request));
            log.info("User logged out: " + user.getEmail());
        }
        
        session.invalidate();
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
