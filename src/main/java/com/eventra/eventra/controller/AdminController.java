package com.eventra.eventra.controller;

import com.eventra.eventra.model.User;
import com.eventra.eventra.dto.UserRegistrationDTO;
import com.eventra.eventra.enums.RoleEnum;
import com.eventra.eventra.service.UserService;
import com.eventra.eventra.service.ClubService;
import com.eventra.eventra.service.EventService;
import com.eventra.eventra.service.ReportService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.logging.Logger;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = Logger.getLogger(AdminController.class.getName());

    private final UserService userService;
    private final ClubService clubService;
    private final EventService eventService;
    private final ReportService reportService;

    public AdminController(UserService userService, ClubService clubService,
                           EventService eventService, ReportService reportService) {
        this.userService = userService;
        this.clubService = clubService;
        this.eventService = eventService;
        this.reportService = reportService;
    }

    /**
     * Verify admin access
     */
    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        return user != null && user.getRole().getRoleName() == RoleEnum.ADMIN;
    }

    /**
     * Manage users page
     */
    @GetMapping("/users")
    public String manageUsers(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        var users = userService.getAllActiveUsers();
        model.addAttribute("users", users);

        return "admin/manage-users";
    }

    /**
     * Add new user (Admin)
     */
    @GetMapping("/users/add")
    public String showAddUserForm(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        model.addAttribute("userRegistrationDTO", new UserRegistrationDTO());
        model.addAttribute("roles", RoleEnum.values());
        return "admin/add-user";
    }

    /**
     * Create new user
     */
    @PostMapping("/users/add")
    public String addUser(@Valid @ModelAttribute UserRegistrationDTO dto,
                         BindingResult result, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        if (result.hasErrors()) {
            model.addAttribute("roles", RoleEnum.values());
            return "admin/add-user";
        }

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            model.addAttribute("error", "Passwords do not match");
            model.addAttribute("roles", RoleEnum.values());
            return "admin/add-user";
        }

        try {
            userService.registerUser(dto);
            log.info(String.format("New user created by admin: %s", dto.getEmail()));
            return "redirect:/admin/users";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", RoleEnum.values());
            return "admin/add-user";
        }
    }

    /**
     * Manage clubs
     */
    @GetMapping("/clubs")
    public String manageClubes(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        var clubs = clubService.getAllClubs();
        model.addAttribute("clubs", clubs);

        return "admin/manage-clubs";
    }

    /**
     * Add new club
     */
    @GetMapping("/clubs/add")
    public String showAddClubForm(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        var clubHeads = userService.getUsersByRole(RoleEnum.CLUB_HEAD);
        model.addAttribute("clubHeads", clubHeads);
        return "admin/add-club";
    }

    /**
     * Create club
     */
    @PostMapping("/clubs/add")
    public String addClub(@RequestParam String clubName,
                         @RequestParam String description,
                         @RequestParam Long clubHeadId,
                         HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        try {
            Optional<User> clubHead = userService.getUserById(clubHeadId);
            if (clubHead.isEmpty()) {
                throw new RuntimeException("Club head not found");
            }

            clubService.createClub(clubName, description, clubHead.get());
            log.info(String.format("Club created: %s", clubName));
            return "redirect:/admin/clubs";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            var clubHeads = userService.getUsersByRole(RoleEnum.CLUB_HEAD);
            model.addAttribute("clubHeads", clubHeads);
            return "admin/add-club";
        }
    }

    /**
     * View pending events for approval
     */
    @GetMapping("/events/pending")
    public String viewPendingEvents(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        var pendingEvents = eventService.getPendingEvents();
        model.addAttribute("pendingEvents", pendingEvents);

        return "admin/pending-events";
    }

    /**
     * View event details for approval
     */
    @GetMapping("/events/{eventId}/review")
    public String reviewEvent(@PathVariable Long eventId, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        var event = eventService.getEventById(eventId);
        if (event.isEmpty()) {
            return "error/not-found";
        }

        model.addAttribute("event", event.get());
        return "admin/review-event";
    }

    /**
     * Generate event reports
     */
    @GetMapping("/reports")
    public String viewReports(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        var completedEvents = eventService.getEventsByStatus(
            com.eventra.eventra.enums.EventStatus.COMPLETED
        );

        model.addAttribute("completedEvents", completedEvents);
        return "admin/reports";
    }

    /**
     * View detailed report
     */
    @GetMapping("/reports/{eventId}")
    public String viewEventReport(@PathVariable Long eventId, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        var event = eventService.getEventById(eventId);
        if (event.isEmpty()) {
            return "error/not-found";
        }

        var report = reportService.getEventReport(eventId);

        model.addAttribute("event", event.get());
        model.addAttribute("report", report.orElse(null));

        return "admin/event-report";
    }

    /**
     * Generate report
     */
    @PostMapping("/reports/{eventId}/generate")
    public String generateReport(@PathVariable Long eventId, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        User admin = (User) session.getAttribute("loggedInUser");
        var event = eventService.getEventById(eventId);

        if (event.isEmpty()) {
            return "error/not-found";
        }

        reportService.generateEventReport(eventId, admin);
        log.info(String.format("Report generated for event: %d by admin: %s", eventId, admin.getEmail()));

        return "redirect:/admin/reports/" + eventId;
    }
}
