package com.eventra.eventra.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.eventra.eventra.enums.RoleEnum;
import com.eventra.eventra.model.User;
import com.eventra.eventra.service.ClubService;
import com.eventra.eventra.service.EventService;
import com.eventra.eventra.service.RegistrationService;
import com.eventra.eventra.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final EventService eventService;
    private final RegistrationService registrationService;
    private final ClubService clubService;
    private final UserService userService;

    public DashboardController(EventService eventService, RegistrationService registrationService,
                               ClubService clubService, UserService userService) {
        this.eventService = eventService;
        this.registrationService = registrationService;
        this.clubService = clubService;
        this.userService = userService;
    }

    /**
     * Main dashboard - redirects based on role
     */
    @GetMapping
    public String dashboard(HttpSession session, Model model) {
        try {
            User user = (User) session.getAttribute("loggedInUser");

            if (user == null) {
                return "redirect:/auth/login";
            }

            // Refresh user from database to get latest approval status
            user = userService.getUserById(user.getUserId()).orElse(user);

            // Update session with refreshed user
            session.setAttribute("loggedInUser", user);

            // Verify role is loaded
            if (user.getRole() == null || user.getRole().getRoleName() == null) {
                model.addAttribute("error", "User role not assigned. Please contact administrator.");
                return "error/unauthorized";
            }

            // Check approval status for CLUB_HEAD and ADMIN roles
            RoleEnum role = user.getRole().getRoleName();
            if ((role == RoleEnum.CLUB_HEAD || role == RoleEnum.ADMIN) && !user.isApproved()) {
                model.addAttribute("message", "Your account is pending admin approval. Please wait for approval notification.");
                model.addAttribute("status", user.getApprovalStatus() != null ? user.getApprovalStatus().getDisplayName() : "PENDING");
                return "error/waiting-approval";
            }

            switch (role) {
                case PARTICIPANT:
                    return studentDashboard(user, model);
                case CLUB_HEAD:
                    return clubHeadDashboard(user, model);
                case ADMIN:
                    return adminDashboard(model);
                default:
                    return "redirect:/auth/login";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error loading dashboard: " + e.getMessage());
            return "error/not-found";
        }
    }

    /**
     * Student Dashboard
     */
    private String studentDashboard(User student, Model model) {
        try {
            var registrations = registrationService.getStudentRegistrations(student.getUserId());
            model.addAttribute("registrations", registrations);
            model.addAttribute("myRegistrationsCount", registrations.size());
            model.addAttribute("upcomingEvents", eventService.getUpcomingApprovedEvents());
            return "dashboard/student-dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading student dashboard: " + e.getMessage());
            return "error/not-found";
        }
    }

    /**
     * Club Head Dashboard
     */
    private String clubHeadDashboard(User clubHead, Model model) {
        try {
            var club = clubService.getClubByClubHead(clubHead.getUserId());

            if (club.isEmpty()) {
                model.addAttribute("error", "You are not assigned as a club head");
                return "error/not-assigned";
            }

            var events = eventService.getEventsByCreator(clubHead.getUserId());
            var pendingEvents = events.stream()
                .filter(e -> e.getStatus().name().equals("PENDING"))
                .toList();
            var approvedEvents = events.stream()
                .filter(e -> e.getStatus().name().equals("APPROVED"))
                .toList();

            long totalRegistrations = events.stream()
                .mapToLong(e -> registrationService.getEventRegistrations(e.getEventId()).size())
                .sum();

            model.addAttribute("club", club.get());
            model.addAttribute("totalEvents", events.size());
            model.addAttribute("pendingEvents", pendingEvents.size());
            model.addAttribute("approvedEvents", approvedEvents.size());
            model.addAttribute("totalRegistrations", totalRegistrations);
            model.addAttribute("events", events);
            model.addAttribute("approvedEventsList", eventService.getUpcomingApprovedEvents());

            return "dashboard/clubhead-dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading club head dashboard: " + e.getMessage());
            return "error/not-found";
        }
    }

    /**
     * Admin Dashboard
     */
    private String adminDashboard(Model model) {
        try {
            long pendingEventCount = eventService.getEventCountByStatus(
                com.eventra.eventra.enums.EventStatus.PENDING
            );
            long approvedEventCount = eventService.getEventCountByStatus(
                com.eventra.eventra.enums.EventStatus.APPROVED
            );
            long completedEventCount = eventService.getEventCountByStatus(
                com.eventra.eventra.enums.EventStatus.COMPLETED
            );

            long pendingUserCount = userService.getPendingUserCount();

            var pendingEvents = eventService.getPendingEvents();
            var approvedEventsList = eventService.getUpcomingApprovedEvents();
            var clubs = clubService.getAllActiveClubs();

            model.addAttribute("pendingEventCount", pendingEventCount);
            model.addAttribute("approvedEventCount", approvedEventCount);
            model.addAttribute("completedEventCount", completedEventCount);
            model.addAttribute("pendingUserCount", pendingUserCount);
            model.addAttribute("pendingEvents", pendingEvents);
            model.addAttribute("approvedEventsList", approvedEventsList);
            model.addAttribute("clubs", clubs);
            model.addAttribute("totalClubs", clubs.size());

            return "dashboard/admin-dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading admin dashboard: " + e.getMessage());
            return "error/not-found";
        }
    }
}
