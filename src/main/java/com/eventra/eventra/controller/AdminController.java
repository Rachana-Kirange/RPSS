package com.eventra.eventra.controller;

import com.eventra.eventra.model.User;
import com.eventra.eventra.model.AuditLog;
import com.eventra.eventra.dto.UserRegistrationDTO;
import com.eventra.eventra.enums.RoleEnum;
import com.eventra.eventra.enums.UserStatus;
import com.eventra.eventra.service.UserService;
import com.eventra.eventra.service.AdminService;
import com.eventra.eventra.service.ClubService;
import com.eventra.eventra.service.EventService;

import com.eventra.eventra.service.AnalyticsService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * AdminController handles administrative functions:
 * - User management (view, activate/deactivate, role assignment)
 * - Club management and assignment
 * - Event approval
 * - Report generation
 * - Audit log viewing
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = Logger.getLogger(AdminController.class.getName());

    private final UserService userService;
    private final AdminService adminService;
    private final ClubService clubService;
    private final EventService eventService;
    private final AnalyticsService analyticsService;

    public AdminController(UserService userService, AdminService adminService, ClubService clubService,
                           EventService eventService, AnalyticsService analyticsService) {
        this.userService = userService;
        this.adminService = adminService;
        this.clubService = clubService;
        this.eventService = eventService;
        this.analyticsService = analyticsService;
    }

    /**
     * Verify admin access
     */
    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        return user != null && user.getRole().getRoleName() == RoleEnum.ADMIN;
    }

    /**
     * Admin dashboard
     */
    @GetMapping("/dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        model.addAttribute("totalUsers", adminService.getAllUsers().size());
        model.addAttribute("participantCount", adminService.countUsersByRole(RoleEnum.PARTICIPANT));
        model.addAttribute("clubHeadCount", adminService.countUsersByRole(RoleEnum.CLUB_HEAD));
        model.addAttribute("adminCount", adminService.countUsersByRole(RoleEnum.ADMIN));
        model.addAttribute("pendingApprovals", adminService.countPendingApprovals());
        model.addAttribute("recentAuditLogs", adminService.getRecentAuditLogs(10));

        return "dashboard/admin-dashboard";
    }

    /**
     * Manage users page
     */
    @GetMapping("/users")
    public String manageUsers(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        List<User> allUsers = adminService.getAllUsers();
        model.addAttribute("users", allUsers);
        model.addAttribute("roles", RoleEnum.values());

        return "admin/manage-users";
    }

    /**
     * View user details
     */
    @GetMapping("/users/{userId}")
    public String viewUserDetails(@PathVariable Long userId, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        Optional<User> user = userService.getUserById(userId);
        if (user.isEmpty()) {
            return "error/not-found";
        }

        List<AuditLog> auditLogs = adminService.getUserAuditLogs(userId);
        model.addAttribute("user", user.get());
        model.addAttribute("auditLogs", auditLogs);
        model.addAttribute("roles", RoleEnum.values());

        return "admin/user-details";
    }

    /**
     * Add new user (Admin-only registration with role selection)
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
     * Create new user with admin-selected role
     */
    @PostMapping("/users/add")
    public String addUser(@Valid @ModelAttribute UserRegistrationDTO dto,
                         @RequestParam(required = false) String role,
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

        if (role == null || role.trim().isEmpty()) {
            model.addAttribute("error", "Please select a role");
            model.addAttribute("roles", RoleEnum.values());
            return "admin/add-user";
        }

        try {
            User admin = (User) session.getAttribute("loggedInUser");
            RoleEnum selectedRole = RoleEnum.valueOf(role.toUpperCase());
            adminService.createUserAsAdmin(dto, selectedRole, admin);
            log.info(String.format("New user created by admin: %s with role: %s", dto.getEmail(), selectedRole));
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            log.warning("Invalid role: " + role);
            model.addAttribute("error", "Invalid role selected");
            model.addAttribute("roles", RoleEnum.values());
            return "admin/add-user";
        } catch (Exception e) {
            log.warning("Error creating user: " + e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", RoleEnum.values());
            return "admin/add-user";
        }
    }

    /**
     * Activate user account
     */
    @PostMapping("/users/{userId}/activate")
    public String activateUser(@PathVariable Long userId, HttpSession session, RedirectAttributes redirect) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        try {
            User admin = (User) session.getAttribute("loggedInUser");
            adminService.activateUser(userId, admin);
            redirect.addFlashAttribute("success", "User activated successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/users/" + userId;
    }

    /**
     * Deactivate user account
     */
    @PostMapping("/users/{userId}/deactivate")
    public String deactivateUser(@PathVariable Long userId, HttpSession session, RedirectAttributes redirect) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        try {
            User admin = (User) session.getAttribute("loggedInUser");
            adminService.deactivateUser(userId, admin);
            redirect.addFlashAttribute("success", "User deactivated successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/users/" + userId;
    }

    /**
     * Assign/upgrade role to user
     * Role hierarchy: PARTICIPANT → CLUB_HEAD → ADMIN
     */
    @PostMapping("/users/{userId}/assign-role")
    public String assignRole(@PathVariable Long userId, 
                            @RequestParam RoleEnum newRole,
                            HttpSession session, RedirectAttributes redirect) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        try {
            User admin = (User) session.getAttribute("loggedInUser");
            adminService.assignRole(userId, newRole, admin);
            redirect.addFlashAttribute("success", "Role assigned successfully to user. Role: " + newRole);
            log.info(String.format("Admin %s assigned role %s to user %d", admin.getEmail(), newRole, userId));
        } catch (Exception e) {
            log.warning(String.format("Error assigning role: %s", e.getMessage()));
            redirect.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/users/" + userId;
    }

    /**
     * View all clubs
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

        var clubHeads = adminService.getUsersByRole(RoleEnum.CLUB_HEAD);
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

            // Verify club head has CLUB_HEAD role
            if (clubHead.get().getRole().getRoleName() != RoleEnum.CLUB_HEAD) {
                throw new RuntimeException("Selected user is not a CLUB_HEAD. Please assign CLUB_HEAD role first.");
            }

            clubService.createClub(clubName, description, clubHead.get());
            log.info(String.format("Club created: %s assigned to %s", clubName, clubHead.get().getEmail()));
            return "redirect:/admin/clubs";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            var clubHeads = adminService.getUsersByRole(RoleEnum.CLUB_HEAD);
            model.addAttribute("clubHeads", clubHeads);
            return "admin/add-club";
        }
    }

    /**
     * View audit logs
     */
    @GetMapping("/audit-logs")
    public String viewAuditLogs(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        List<AuditLog> auditLogs = adminService.getRecentAuditLogs(100);
        model.addAttribute("auditLogs", auditLogs);

        return "admin/audit-logs";
    }

    /**
     * View audit logs for specific user
     */
    @GetMapping("/audit-logs/user/{userId}")
    public String viewUserAuditLogs(@PathVariable Long userId, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        Optional<User> user = userService.getUserById(userId);
        if (user.isEmpty()) {
            return "error/not-found";
        }

        List<AuditLog> auditLogs = adminService.getUserAuditLogs(userId);
        model.addAttribute("user", user.get());
        model.addAttribute("auditLogs", auditLogs);

        return "admin/user-audit-logs";
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

        model.addAttribute("event", event.get());
        model.addAttribute("report", event.get().getEventReport());

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

        // Report data can be accessed from event.getEventReport()
        log.info("Report generated for event: " + eventId + " by admin: " + admin.getEmail());

        return "redirect:/admin/reports/" + eventId;
    }

    /**
     * Review report form
     */
    @GetMapping("/reports/{eventId}/review")
    public String showReportReviewForm(@PathVariable Long eventId, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        var event = eventService.getEventById(eventId);
        if (event.isEmpty()) {
            return "error/not-found";
        }

        model.addAttribute("event", event.get());
        model.addAttribute("report", event.get().getEventReport());
        model.addAttribute("reportStatus", event.get().getReportStatus());

        return "admin/review-report";
    }

    /**
     * Approve or reject report
     */
    @PostMapping("/reports/{eventId}/review")
    public String reviewReport(@PathVariable Long eventId,
                              @RequestParam String action,
                              @RequestParam(required = false) String comments,
                              HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        User admin = (User) session.getAttribute("loggedInUser");
        var event = eventService.getEventById(eventId);

        if (event.isEmpty()) {
            return "error/not-found";
        }

        try {
            if ("approve".equals(action)) {
                event.get().setReportStatus(com.eventra.eventra.enums.ReportStatus.APPROVED);
                event.get().setReportReviewedBy(admin);
                event.get().setReportReviewComments(comments);
                log.info(String.format("Report for event %d approved by admin %s", eventId, admin.getEmail()));
            } else if ("reject".equals(action)) {
                event.get().setReportStatus(com.eventra.eventra.enums.ReportStatus.REJECTED);
                event.get().setReportReviewedBy(admin);
                event.get().setReportReviewComments(comments);
                log.info(String.format("Report for event %d rejected by admin %s", eventId, admin.getEmail()));
            } else if ("revisions".equals(action)) {
                event.get().setReportStatus(com.eventra.eventra.enums.ReportStatus.REVISIONS_NEEDED);
                event.get().setReportReviewedBy(admin);
                event.get().setReportReviewComments(comments);
                log.info(String.format("Report for event %d marked for revisions by admin %s", eventId, admin.getEmail()));
            }

            eventService.updateEvent(event.get());
            return "redirect:/admin/reports";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("event", event.get());
            model.addAttribute("report", event.get().getEventReport());
            model.addAttribute("reportStatus", event.get().getReportStatus());
            return "admin/review-report";
        }
    }

    /**
     * View pending user approvals (if using approval workflow)
     */
    @GetMapping("/users/pending")
    public String viewPendingApprovals(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        List<User> pendingUsers = adminService.getUsersByApprovalStatus(UserStatus.PENDING);
        model.addAttribute("pendingUsers", pendingUsers);
        model.addAttribute("totalPending", pendingUsers.size());

        return "admin/users-pending";
    }

    /**
     * Approve user registration
     */
    @PostMapping("/users/{userId}/approve")
    public String approveUser(@PathVariable Long userId, HttpSession session, RedirectAttributes redirect) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        try {
            User admin = (User) session.getAttribute("loggedInUser");
            adminService.approveUser(userId, admin);
            log.info(String.format("User %d approved by admin %s", userId, admin.getEmail()));
            redirect.addFlashAttribute("success", "User approved successfully");
            return "redirect:/admin/users/pending";
        } catch (Exception e) {
            log.warning(String.format("Error approving user: %s", e.getMessage()));
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users/pending";
        }
    }

    /**
     * Reject user registration
     */
    @PostMapping("/users/{userId}/reject")
    public String rejectUser(@PathVariable Long userId, 
                            @RequestParam(value = "reason", required = false) String reason,
                            HttpSession session, RedirectAttributes redirect) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        try {
            User admin = (User) session.getAttribute("loggedInUser");
            String rejectionReason = reason != null && !reason.isEmpty() ? reason : "Rejected by admin";
            adminService.rejectUser(userId, rejectionReason, admin);
            log.info(String.format("User %d rejected by admin %s", userId, admin.getEmail()));
            redirect.addFlashAttribute("success", "User rejected successfully");
            return "redirect:/admin/users/pending";
        } catch (Exception e) {
            log.warning(String.format("Error rejecting user: %s", e.getMessage()));
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users/pending";
        }
    }

    /**
     * Show form to assign club to a CLUB_HEAD user
     */
    @GetMapping("/users/{userId}/assign-club-form")
    public String showAssignClubForm(@PathVariable Long userId, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        try {
            User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Verify user is a CLUB_HEAD
            if (user.getRole().getRoleName() != RoleEnum.CLUB_HEAD) {
                model.addAttribute("error", "This user is not a CLUB_HEAD. Please assign CLUB_HEAD role first.");
                return "admin/assign-club-form";
            }

            List<com.eventra.eventra.model.Club> clubs = clubService.getAllClubs();
            model.addAttribute("user", user);
            model.addAttribute("clubs", clubs);
            return "admin/assign-club-form";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "admin/assign-club-form";
        }
    }

    /**
     * Assign club to a CLUB_HEAD user
     */
    @PostMapping("/users/{userId}/assign-club")
    public String assignClubToUser(@PathVariable Long userId, 
                                   @RequestParam Long clubId,
                                   HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        try {
            User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Verify user is CLUB_HEAD and approved
            if (user.getRole().getRoleName() != RoleEnum.CLUB_HEAD) {
                throw new RuntimeException("This user is not a CLUB_HEAD");
            }

            if (!user.isApproved()) {
                throw new RuntimeException("Club Head must be approved before assigning club");
            }
            
            // Assign club to user
            clubService.updateClubHead(clubId, user);
            log.info(String.format("Club %d assigned to user %s (ID: %d)", clubId, user.getEmail(), userId));
            
            return "redirect:/admin/users";
        } catch (Exception e) {
            log.warning(String.format("Error assigning club: %s", e.getMessage()));
            model.addAttribute("error", e.getMessage());
            try {
                User user = userService.getUserById(userId).orElse(null);
                model.addAttribute("user", user);
                List<com.eventra.eventra.model.Club> clubs = clubService.getAllClubs();
                model.addAttribute("clubs", clubs);
            } catch (Exception ex) {
                // silently fail
            }
            return "admin/assign-club-form";
        }
    }

    /**
     * View Analytics Dashboard
     */
    @GetMapping("/analytics")
    public String viewAnalytics(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard";
        }

        try {
            var analytics = analyticsService.getOverallAnalytics();
            model.addAttribute("analytics", analytics);
            log.info("Admin analytics dashboard accessed");
            return "admin/analytics";
        } catch (Exception e) {
            model.addAttribute("error", String.format("Error loading analytics: %s", e.getMessage()));
            return "error/not-found";
        }
    }
}

