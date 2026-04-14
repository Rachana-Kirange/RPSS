    package com.eventra.eventra.controller;

    import com.eventra.eventra.model.User;
    import com.eventra.eventra.model.Event;
    import com.eventra.eventra.dto.EventCreateDTO;
    import com.eventra.eventra.service.EventService;
    import com.eventra.eventra.service.RegistrationService;
    import com.eventra.eventra.service.ClubService;
    import jakarta.servlet.http.HttpSession;
    import jakarta.validation.Valid;
    import org.springframework.stereotype.Controller;
    import org.springframework.ui.Model;
    import org.springframework.validation.BindingResult;
    import org.springframework.web.bind.annotation.*;

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
        private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

        public EventController(EventService eventService, RegistrationService registrationService,
                            ClubService clubService) {
            this.eventService = eventService;
            this.registrationService = registrationService;
            this.clubService = clubService;
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

            if (user == null || !user.getRole().getRoleName().name().equals("CLUB_HEAD")) {
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

            var club = clubService.getClubByClubHead(user.getUserId());
            if (club.isEmpty()) {
                model.addAttribute("error", "You are not assigned to any club");
                return "error/not-assigned";
            }

            if (result.hasErrors()) {
                model.addAttribute("club", club.get());
                return "event/create-event";
            }

            try {
                LocalDateTime eventDateTime = LocalDateTime.parse(dto.getEventDateTime(), dateTimeFormatter);

                Event event = eventService.createEvent(
                    dto.getTitle(),
                    dto.getDescription(),
                    eventDateTime,
                    dto.getVenue(),
                    dto.getMaxCapacity(),
                    club.get().getClubId(),
                    user
                );

                log.info(String.format("Event created: %d by %s", event.getEventId(), user.getEmail()));
                return "redirect:/dashboard";
            } catch (Exception e) {
                model.addAttribute("error", "Error creating event: " + e.getMessage());
                model.addAttribute("club", club.get());
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
    }
