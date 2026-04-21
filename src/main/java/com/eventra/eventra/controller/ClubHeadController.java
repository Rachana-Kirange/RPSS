package com.eventra.eventra.controller;

import com.eventra.eventra.model.User;
import com.eventra.eventra.model.Event;
import com.eventra.eventra.model.Club;
import com.eventra.eventra.dto.EventCreateDTO;
import com.eventra.eventra.service.EventService;
import com.eventra.eventra.service.RegistrationService;
import com.eventra.eventra.service.ClubService;
import com.eventra.eventra.service.MediaService;
import com.eventra.eventra.enums.EventStatus;
import com.eventra.eventra.enums.MediaFileType;
import com.eventra.eventra.enums.RoleEnum;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * ClubHeadController handles all club head specific operations
 * 
 * Core Responsibilities:
 * 1. Event Management: Create events with activity proposals
 * 2. Event Approval: Send events for admin approval (automatic)
 * 3. Participant Management: View and manage event registrations
 * 4. Media Management: Upload photos and videos after event
 * 5. Reporting: Generate and view event reports
 * 
 * Security: All operations restricted to CLUB_HEAD role and assigned club
 */
@Controller
@RequestMapping("/clubhead")
public class ClubHeadController {

    private static final Logger log = Logger.getLogger(ClubHeadController.class.getName());

    private final EventService eventService;
    private final RegistrationService registrationService;
    private final ClubService clubService;
    private final MediaService mediaService;
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    public ClubHeadController(EventService eventService, RegistrationService registrationService,
                             ClubService clubService, MediaService mediaService) {
        this.eventService = eventService;
        this.registrationService = registrationService;
        this.clubService = clubService;
        this.mediaService = mediaService;
    }

    /**
     * Verify user is an approved club head
     */
    private boolean isApprovedClubHead(User user) {
        return user != null
            && user.getRole() != null
            && user.getRole().getRoleName() == RoleEnum.CLUB_HEAD
            && user.isApproved();
    }

    /**
     * Club Head Dashboard - Overview of all club activities
     */
    @GetMapping("/dashboard")
    public String clubHeadDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");

        if (!isApprovedClubHead(user)) {
            return "redirect:/dashboard";
        }

        try {
            Optional<Club> club = clubService.getClubByClubHead(user.getUserId());
            if (club.isEmpty()) {
                model.addAttribute("error", "You are not assigned to any club");
                return "error/not-assigned";
            }

            var events = eventService.getEventsByCreator(user.getUserId());
            var pendingEvents = events.stream()
                .filter(e -> e.getStatus() == EventStatus.PENDING)
                .toList();
            var approvedEvents = events.stream()
                .filter(e -> e.getStatus() == EventStatus.APPROVED)
                .toList();
            var completedEvents = events.stream()
                .filter(e -> e.getStatus() == EventStatus.COMPLETED)
                .toList();

            long totalRegistrations = events.stream()
                .mapToLong(e -> registrationService.getEventRegistrations(e.getEventId()).size())
                .sum();

            model.addAttribute("club", club.get());
            model.addAttribute("totalEvents", events.size());
            model.addAttribute("pendingEvents", pendingEvents.size());
            model.addAttribute("approvedEvents", approvedEvents.size());
            model.addAttribute("completedEvents", completedEvents.size());
            model.addAttribute("totalRegistrations", totalRegistrations);
            model.addAttribute("events", events);

            return "dashboard/clubhead-dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading club head dashboard: " + e.getMessage());
            return "error/not-found";
        }
    }

    /**
     * Create Event Page
     * Club head can create new events with activity proposal
     */
    @GetMapping("/events/create")
    public String showCreateEventForm(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");

        if (!isApprovedClubHead(user)) {
            return "redirect:/dashboard";
        }

        Optional<Club> club = clubService.getClubByClubHead(user.getUserId());
        if (club.isEmpty()) {
            model.addAttribute("error", "You are not assigned to any club");
            return "error/not-assigned";
        }

        model.addAttribute("club", club.get());
        model.addAttribute("eventCreateDTO", new EventCreateDTO());
        return "event/create-event";
    }

    /**
     * Create Event
     * Event is automatically sent for admin approval (status = PENDING)
     */
    @PostMapping("/events/create")
    public String createEvent(@Valid @ModelAttribute EventCreateDTO dto,
                             BindingResult result, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");

        if (!isApprovedClubHead(user)) {
            return "redirect:/dashboard";
        }

        try {
            Optional<Club> club = clubService.getClubByClubHead(user.getUserId());
            if (club.isEmpty()) {
                model.addAttribute("error", "You are not assigned to any club");
                return "error/not-assigned";
            }

            if (result.hasErrors()) {
                model.addAttribute("club", club.get());
                return "event/create-event";
            }

            LocalDateTime eventDateTime = LocalDateTime.parse(dto.getEventDateTime(), dateTimeFormatter);
            LocalDateTime endDateTime = dto.getEndDateTime() != null && !dto.getEndDateTime().isBlank()
                ? LocalDateTime.parse(dto.getEndDateTime(), dateTimeFormatter)
                : eventDateTime;

            if (endDateTime.isBefore(eventDateTime)) {
                model.addAttribute("error", "End date/time cannot be before start date/time");
                model.addAttribute("club", club.get());
                return "event/create-event";
            }

            Event event = eventService.createEvent(
                dto.getTitle(),
                dto.getDescription(),
                eventDateTime,
                endDateTime,
                dto.getVenue(),
                dto.getMaxCapacity(),
                club.get().getClubId(),
                user,
                dto.getRequiresPayment(),
                dto.getPaymentAmount(),
                dto.getRequiresQR(),
                dto.getActivityProposal()
            );

            log.info(String.format("Event created: %d by club head: %s, Status: PENDING (awaiting admin approval)", 
                event.getEventId(), user.getEmail()));
            model.addAttribute("success", "Event created successfully! It has been sent for admin approval.");
            return "redirect:/clubhead/dashboard";
        } catch (Exception e) {
            log.severe("Error creating event for club head " + user.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
            String message = e.getMessage();
            if (message != null && (message.contains("Unknown column") || message.contains("end_date"))) {
                message = "Database schema is not updated for new event fields. Restart the application to apply schema changes.";
            }
            model.addAttribute("error", "Error creating event: " + message);
            model.addAttribute("club", clubService.getClubByClubHead(user.getUserId()).orElse(null));
            model.addAttribute("eventCreateDTO", dto);
            return "event/create-event";
        }
    }

    /**
     * Manage Participants for a specific event
     * View all registered participants and their details
     */
    @GetMapping({"/events/{eventId}/participants", "/events/{eventId}/registered-users"})
    public String manageParticipants(@PathVariable Long eventId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");

        if (!isApprovedClubHead(user)) {
            return "redirect:/dashboard";
        }

        Optional<Event> event = eventService.getEventById(eventId);
        if (event.isEmpty()) {
            model.addAttribute("error", "Event not found");
            return "error/not-found";
        }

        // Verify club head owns this event
        if (!event.get().getCreatedBy().getUserId().equals(user.getUserId())) {
            return "redirect:/clubhead/dashboard";
        }

        var registrations = registrationService.getEventRegistrations(eventId);
        model.addAttribute("event", event.get());
        model.addAttribute("registrations", registrations);
        model.addAttribute("participantCount", registrations.size());

        return "event/manage-participants";
    }

    /**
     * Media Upload Page
     * Club head can upload photos and videos after the event
     */
    @GetMapping("/events/{eventId}/media")
    public String showMediaUploadForm(@PathVariable Long eventId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");

        if (!isApprovedClubHead(user)) {
            return "redirect:/dashboard";
        }

        Optional<Event> event = eventService.getEventById(eventId);
        if (event.isEmpty()) {
            model.addAttribute("error", "Event not found");
            return "error/not-found";
        }

        // Verify club head owns this event
        if (!event.get().getCreatedBy().getUserId().equals(user.getUserId())) {
            return "redirect:/clubhead/dashboard";
        }

        if (event.get().getStatus() != EventStatus.COMPLETED) {
            return "redirect:/clubhead/events/" + eventId;
        }

        model.addAttribute("event", event.get());
        model.addAttribute("media", event.get().getMedia());
        return "event/upload-media";
    }

    /**
     * Upload Media Files
     * Support photos and videos after event completion
     */
    @PostMapping("/events/{eventId}/media/upload")
    public String uploadMedia(@PathVariable Long eventId,
                             @RequestParam("files") MultipartFile[] files,
                             @RequestParam(defaultValue = "IMAGE") MediaFileType mediaType,
                             @RequestParam(required = false) String description,
                             HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");

        if (!isApprovedClubHead(user)) {
            return "redirect:/dashboard";
        }

        Optional<Event> event = eventService.getEventById(eventId);
        if (event.isEmpty()) {
            model.addAttribute("error", "Event not found");
            return "error/not-found";
        }

        // Verify club head owns this event
        if (!event.get().getCreatedBy().getUserId().equals(user.getUserId())) {
            return "redirect:/clubhead/dashboard";
        }

        if (event.get().getStatus() != EventStatus.COMPLETED) {
            return "redirect:/clubhead/events/" + eventId;
        }

        try {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    mediaService.uploadMedia(eventId, file, mediaType, description, user);
                }
            }
            log.info(String.format("Media uploaded for event %d by club head %s", eventId, user.getEmail()));
            return "redirect:/clubhead/events/" + eventId + "/media";
        } catch (Exception e) {
            model.addAttribute("error", "Error uploading media: " + e.getMessage());
            model.addAttribute("event", event.get());
            model.addAttribute("media", event.get().getMedia());
            return "event/upload-media";
        }
    }

    /**
     * Event Report Page
     * View event statistics and generate reports
     */
    @GetMapping("/events/{eventId}/report")
    public String viewEventReport(@PathVariable Long eventId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");

        if (!isApprovedClubHead(user)) {
            return "redirect:/dashboard";
        }

        Optional<Event> event = eventService.getEventById(eventId);
        if (event.isEmpty()) {
            model.addAttribute("error", "Event not found");
            return "error/not-found";
        }

        // Verify club head owns this event
        if (!event.get().getCreatedBy().getUserId().equals(user.getUserId())) {
            return "redirect:/clubhead/dashboard";
        }

        if (event.get().getStatus() != EventStatus.COMPLETED) {
            return "redirect:/clubhead/events/" + eventId;
        }

        var registrations = registrationService.getEventRegistrations(eventId);
        model.addAttribute("event", event.get());
        model.addAttribute("eventReport", event.get().getEventReport());
        model.addAttribute("registrationCount", registrations.size());
        model.addAttribute("feedbackCount", event.get().getFeedbacks().size());

        return "event/view-report";
    }

    /**
     * Save Event Report
     * Club head submits final report after event completion
     */
    @PostMapping("/events/{eventId}/report")
    public String saveEventReport(@PathVariable Long eventId,
                                 @RequestParam String report,
                                 HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");

        if (!isApprovedClubHead(user)) {
            return "redirect:/dashboard";
        }

        Optional<Event> event = eventService.getEventById(eventId);
        if (event.isEmpty()) {
            model.addAttribute("error", "Event not found");
            return "error/not-found";
        }

        // Verify club head owns this event
        if (!event.get().getCreatedBy().getUserId().equals(user.getUserId())) {
            return "redirect:/clubhead/dashboard";
        }

        if (event.get().getStatus() != EventStatus.COMPLETED) {
            return "redirect:/clubhead/events/" + eventId;
        }

        try {
            eventService.saveEventReport(eventId, report);
            log.info(String.format("Event report saved for event: %d by club head: %s", eventId, user.getEmail()));
            return "redirect:/clubhead/events/" + eventId + "/report";
        } catch (Exception e) {
            model.addAttribute("error", "Error saving report: " + e.getMessage());
            model.addAttribute("event", event.get());
            return "event/view-report";
        }
    }

    /**
     * View All Events
     * Club head can view all their created events
     */
    @GetMapping("/events")
    public String viewAllEvents(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");

        if (!isApprovedClubHead(user)) {
            return "redirect:/dashboard";
        }

        try {
            Optional<Club> club = clubService.getClubByClubHead(user.getUserId());
            var events = eventService.getEventsByCreator(user.getUserId());

            model.addAttribute("club", club.orElse(null));
            model.addAttribute("events", events);
            model.addAttribute("totalEvents", events.size());
            return "event/clubhead-events-list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading events: " + e.getMessage());
            return "error/not-found";
        }
    }

    /**
     * Event Details
     * Club head can view detailed information about their event
     */
    @GetMapping("/events/{eventId}")
    public String viewEventDetails(@PathVariable Long eventId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");

        if (!isApprovedClubHead(user)) {
            return "redirect:/dashboard";
        }

        Optional<Event> event = eventService.getEventById(eventId);
        if (event.isEmpty()) {
            model.addAttribute("error", "Event not found");
            return "error/not-found";
        }

        // Verify club head owns this event
        if (!event.get().getCreatedBy().getUserId().equals(user.getUserId())) {
            return "redirect:/clubhead/dashboard";
        }

        var registrations = registrationService.getEventRegistrations(eventId);
        model.addAttribute("event", event.get());
        model.addAttribute("registrations", registrations);
        model.addAttribute("participantCount", registrations.size());
        model.addAttribute("registrationCount", registrations.size());
        model.addAttribute("availableSeats", event.get().getMaxCapacity() - registrations.size());

        return "event/clubhead-event-details";
    }
}
