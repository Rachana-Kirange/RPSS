    package com.eventra.eventra.controller;

    import com.eventra.eventra.model.User;
    import com.eventra.eventra.model.Event;
    import com.eventra.eventra.dto.EventCreateDTO;
    import com.eventra.eventra.enums.EventStatus;
    import com.eventra.eventra.enums.MediaFileType;
    import com.eventra.eventra.enums.RoleEnum;
    import com.eventra.eventra.service.EventService;
    import com.eventra.eventra.service.RegistrationService;
    import com.eventra.eventra.service.ClubService;
    import com.eventra.eventra.service.MediaService;
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

    @Controller
    @RequestMapping("/events")
    public class EventController {

        private static final Logger log = Logger.getLogger(EventController.class.getName());

        private final EventService eventService;
        private final RegistrationService registrationService;
        private final ClubService clubService;
        private final MediaService mediaService;
        private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

        public EventController(EventService eventService, RegistrationService registrationService,
                            ClubService clubService, MediaService mediaService) {
            this.eventService = eventService;
            this.registrationService = registrationService;
            this.clubService = clubService;
            this.mediaService = mediaService;
        }

        /**
         * View all approved events
         */
        @GetMapping
        public String viewAllEvents(Model model) {
            var events = eventService.getUpcomingApprovedEvents();
            model.addAttribute("events", events);
            return "event/events-list";
        }

        /**
         * View event details
         */
        @GetMapping("/{eventId}")
        public String viewEventDetails(@PathVariable Long eventId, HttpSession session, Model model) {
            Optional<Event> event = eventService.getEventById(eventId);

            if (event.isEmpty()) {
                return "redirect:/events";
            }

            User user = (User) session.getAttribute("loggedInUser");
            int registrationCount = registrationService.getEventRegistrations(eventId).size();
            int availableSeats = event.get().getAvailableSeats();

            model.addAttribute("event", event.get());
            model.addAttribute("registrationCount", registrationCount);
            model.addAttribute("availableSeats", availableSeats);

            if (user != null && user.getRole().getRoleName().name().equals("PARTICIPANT")) {
                boolean isRegistered = registrationService.isUserRegistered(eventId, user.getUserId());
                model.addAttribute("isRegistered", isRegistered);
            }

            return "event/event-details";
        }

        /**
         * Create event form (Club Head)
         */
        @GetMapping("/create")
        public String showCreateEventForm(HttpSession session, Model model) {
            User user = (User) session.getAttribute("loggedInUser");

            if (user == null || user.getRole() == null || user.getRole().getRoleName() != RoleEnum.CLUB_HEAD) {
                return "redirect:/dashboard";
            }

            var club = clubService.getClubByClubHead(user.getUserId());
            if (club.isEmpty()) {
                model.addAttribute("error", "You are not assigned to any club");
                return "error/not-assigned";
            }

            model.addAttribute("club", club.get());
            model.addAttribute("eventCreateDTO", new EventCreateDTO());
            return "event/create-event";
        }

        /**
         * Create event (Club Head)
         */
        @PostMapping("/create")
        public String createEvent(@Valid @ModelAttribute EventCreateDTO dto,
                                BindingResult result, HttpSession session, Model model) {
            User user = (User) session.getAttribute("loggedInUser");

            if (user == null) {
                return "redirect:/auth/login";
            }

            try {
                var club = clubService.getClubByClubHead(user.getUserId());
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

                log.info(String.format("Event created: %d by %s", event.getEventId(), user.getEmail()));
                return "redirect:/dashboard";
            } catch (Exception e) {
                log.severe("Error creating event for user " + user.getEmail() + ": " + e.getMessage());
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
         * Pending events list (Admin)
         */
        @GetMapping("/pending")
        public String viewPendingEvents(HttpSession session, Model model) {
            User user = (User) session.getAttribute("loggedInUser");

            if (user == null || !user.getRole().getRoleName().name().equals("ADMIN")) {
                return "redirect:/dashboard";
            }

            var pendingEvents = eventService.getPendingEvents();
            model.addAttribute("pendingEvents", pendingEvents);
            return "event/pending-events";
        }

        /**
         * Approve event (Admin)
         */
        @PostMapping("/{eventId}/approve")
        public String approveEvent(@PathVariable Long eventId, HttpSession session) {
            User admin = (User) session.getAttribute("loggedInUser");

            if (admin == null || !admin.getRole().getRoleName().name().equals("ADMIN")) {
                return "redirect:/dashboard";
            }

            eventService.approveEvent(eventId, admin);
            log.info(String.format("Event approved: %d by admin: %s", eventId, admin.getEmail()));
            return "redirect:/events/pending";
        }

        /**
         * Reject event (Admin)
         */
        @PostMapping("/{eventId}/reject")
        public String rejectEvent(@PathVariable Long eventId,
                                @RequestParam String reason,
                                HttpSession session) {
            User admin = (User) session.getAttribute("loggedInUser");

            if (admin == null || !admin.getRole().getRoleName().name().equals("ADMIN")) {
                return "redirect:/dashboard";
            }

            eventService.rejectEvent(eventId, admin, reason);
            log.warning(String.format("Event rejected: %d - Reason: %s", eventId, reason));
            return "redirect:/events/pending";
        }

        /**
         * View event participants (Club Head)
         */
        @GetMapping({"/{eventId}/participants", "/{eventId}/registered-users"})
        public String viewParticipants(@PathVariable Long eventId, HttpSession session, Model model) {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return "redirect:/auth/login";
            }

            Optional<Event> event = eventService.getEventById(eventId);
            if (event.isEmpty()) {
                model.addAttribute("error", "Event not found");
                return "error/not-found";
            }

            // Check if user is the event creator or admin
            if (!event.get().getCreatedBy().getUserId().equals(user.getUserId()) && 
                !user.getRole().getRoleName().name().equals("ADMIN")) {
                return "redirect:/dashboard";
            }

            var registrations = registrationService.getEventRegistrations(eventId);
            model.addAttribute("event", event.get());
            model.addAttribute("registrations", registrations);
            model.addAttribute("participantCount", registrations.size());
            return "event/manage-participants";
        }

        /**
         * Media upload page (Club Head)
         */
        @GetMapping("/{eventId}/media")
        public String viewMediaUpload(@PathVariable Long eventId, HttpSession session, Model model) {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return "redirect:/auth/login";
            }

            Optional<Event> event = eventService.getEventById(eventId);
            if (event.isEmpty()) {
                model.addAttribute("error", "Event not found");
                return "error/not-found";
            }

            // Check if user is the event creator
            if (!event.get().getCreatedBy().getUserId().equals(user.getUserId())) {
                return "redirect:/dashboard";
            }

            if (event.get().getStatus() != EventStatus.COMPLETED) {
                return "redirect:/events/" + eventId;
            }

            model.addAttribute("event", event.get());
            model.addAttribute("media", event.get().getMedia());
            return "event/upload-media";
        }

        /**
         * Upload media for an event (Club Head)
         */
        @PostMapping("/{eventId}/media/upload")
        public String uploadMedia(@PathVariable Long eventId,
                                  @RequestParam("files") MultipartFile[] files,
                                  @RequestParam(required = false) String description,
                                  HttpSession session,
                                  Model model) {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return "redirect:/auth/login";
            }

            Optional<Event> event = eventService.getEventById(eventId);
            if (event.isEmpty()) {
                model.addAttribute("error", "Event not found");
                return "error/not-found";
            }

            if (!event.get().getCreatedBy().getUserId().equals(user.getUserId())) {
                return "redirect:/dashboard";
            }

            if (event.get().getStatus() != EventStatus.COMPLETED) {
                return "redirect:/events/" + eventId;
            }

            try {
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        mediaService.uploadMedia(eventId, file, inferMediaType(file), description, user);
                    }
                }

                return "redirect:/events/" + eventId + "/media";
            } catch (Exception e) {
                model.addAttribute("error", "Error uploading media: " + e.getMessage());
                model.addAttribute("event", event.get());
                model.addAttribute("media", event.get().getMedia());
                return "event/upload-media";
            }
        }

        /**
         * Event report page (Club Head)
         */
        @GetMapping("/{eventId}/report")
        public String viewEventReport(@PathVariable Long eventId, HttpSession session, Model model) {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return "redirect:/auth/login";
            }

            Optional<Event> event = eventService.getEventById(eventId);
            if (event.isEmpty()) {
                model.addAttribute("error", "Event not found");
                return "error/not-found";
            }

            // Check if user is the event creator
            if (!event.get().getCreatedBy().getUserId().equals(user.getUserId())) {
                return "redirect:/dashboard";
            }

            if (event.get().getStatus() != EventStatus.COMPLETED) {
                return "redirect:/events/" + eventId;
            }

            model.addAttribute("event", event.get());
            model.addAttribute("eventReport", event.get().getEventReport());
            model.addAttribute("registrationCount", event.get().getRegistrationCount());
            model.addAttribute("feedbackCount", event.get().getFeedbacks().size());
            return "event/view-report";
        }

        /**
         * Save event report (Club Head)
         */
        @PostMapping("/{eventId}/report")
        public String saveEventReport(@PathVariable Long eventId, 
                                     @RequestParam String report,
                                     HttpSession session, Model model) {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return "redirect:/auth/login";
            }

            Optional<Event> event = eventService.getEventById(eventId);
            if (event.isEmpty()) {
                model.addAttribute("error", "Event not found");
                return "error/not-found";
            }

            // Check if user is the event creator
            if (!event.get().getCreatedBy().getUserId().equals(user.getUserId())) {
                return "redirect:/dashboard";
            }

            if (event.get().getStatus() != EventStatus.COMPLETED) {
                return "redirect:/events/" + eventId;
            }

            eventService.saveEventReport(eventId, report);
            log.info(String.format("Event report saved for event: %d", eventId));
            return "redirect:/events/" + eventId + "/report";
        }

        private MediaFileType inferMediaType(MultipartFile file) {
            String contentType = file.getContentType();
            if (contentType != null) {
                if (contentType.startsWith("image/")) {
                    return MediaFileType.IMAGE;
                }
                if (contentType.startsWith("video/")) {
                    return MediaFileType.VIDEO;
                }
            }

            String fileName = file.getOriginalFilename();
            if (fileName != null) {
                String lowerCaseName = fileName.toLowerCase();
                if (lowerCaseName.endsWith(".png") || lowerCaseName.endsWith(".jpg")
                    || lowerCaseName.endsWith(".jpeg") || lowerCaseName.endsWith(".gif")
                    || lowerCaseName.endsWith(".webp")) {
                    return MediaFileType.IMAGE;
                }
                if (lowerCaseName.endsWith(".mp4") || lowerCaseName.endsWith(".mov")
                    || lowerCaseName.endsWith(".avi") || lowerCaseName.endsWith(".mkv")) {
                    return MediaFileType.VIDEO;
                }
            }

            return MediaFileType.DOCUMENT;
        }
    }
