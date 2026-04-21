package com.eventra.eventra.service;

import com.eventra.eventra.model.Event;
import com.eventra.eventra.model.Club;
import com.eventra.eventra.model.User;
import com.eventra.eventra.enums.EventStatus;
import com.eventra.eventra.enums.RoleEnum;
import com.eventra.eventra.repository.EventRepository;
import com.eventra.eventra.repository.ClubRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventService {

    private static final Logger log = Logger.getLogger(EventService.class.getName());

    private final EventRepository eventRepository;
    private final ClubRepository clubRepository;

    public EventService(EventRepository eventRepository, ClubRepository clubRepository) {
        this.eventRepository = eventRepository;
        this.clubRepository = clubRepository;
    }

    /**
     * Create a new event (Club Head)
     */
    public Event createEvent(String title, String description, LocalDateTime eventDate,
                        LocalDateTime endDate, String venue, Integer maxCapacity, Long clubId, User user) {

    // 🔥 ROLE CHECK HERE (MAIN FIX)
    if (user == null || user.getRole() == null || user.getRole().getRoleName() != RoleEnum.CLUB_HEAD) {
        throw new RuntimeException("Only Club Heads can create events");
    }

    Club club = clubRepository.findById(clubId)
        .orElseThrow(() -> new RuntimeException("Club not found"));

    Event event = new Event();
    event.setTitle(title);
    event.setDescription(description);
    event.setEventDate(eventDate);
    event.setEndDate(endDate != null ? endDate : eventDate);
    event.setVenue(venue);
    event.setMaxCapacity(maxCapacity);
    event.setClub(club);
    event.setCreatedBy(user);
    event.setStatus(EventStatus.PENDING);

    return eventRepository.save(event);
}

    /**
     * Create a new event with optional fields (Club Head)
     */
    public Event createEvent(String title, String description, LocalDateTime eventDate,
                        LocalDateTime endDate, String venue, Integer maxCapacity, Long clubId, User user,
                        Boolean requiresPayment, java.math.BigDecimal paymentAmount,
                        Boolean requiresQR, String activityProposal) {

        // 🔥 ROLE CHECK HERE (MAIN FIX)
        if (user == null || user.getRole() == null || user.getRole().getRoleName() != RoleEnum.CLUB_HEAD) {
            throw new RuntimeException("Only Club Heads can create events");
        }

        Club club = clubRepository.findById(clubId)
            .orElseThrow(() -> new RuntimeException("Club not found"));

        LocalDateTime resolvedEndDate = endDate != null ? endDate : eventDate;
        if (resolvedEndDate.isBefore(eventDate)) {
            throw new IllegalArgumentException("End date/time cannot be before start date/time");
        }

        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        event.setEventDate(eventDate);
        event.setEndDate(resolvedEndDate);
        event.setVenue(venue);
        event.setMaxCapacity(maxCapacity);
        event.setClub(club);
        event.setCreatedBy(user);
        event.setStatus(EventStatus.PENDING);
        event.setRequiresPayment(requiresPayment != null ? requiresPayment : false);
        if (requiresPayment != null && requiresPayment && paymentAmount != null) {
            event.setPaymentAmount(paymentAmount);
        }
        event.setRequiresQR(requiresQR != null ? requiresQR : false);
        event.setActivityProposal(activityProposal);

        return eventRepository.save(event);
    }

    /**
     * Approve event (Admin)
     */
    public Event approveEvent(Long eventId, User admin) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        event.approveEvent(admin);
        log.info(String.format("Event approved: %d by admin: %s", eventId, admin.getEmail()));
        return eventRepository.save(event);
    }

    /**
     * Reject event (Admin)
     */
    public Event rejectEvent(Long eventId, User admin, String reason) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        event.rejectEvent(admin, reason);
        log.warning(String.format("Event rejected: %d - Reason: %s", eventId, reason));
        return eventRepository.save(event);
    }

    /**
     * Get event by ID
     */
    public Optional<Event> getEventById(Long eventId) {
        return eventRepository.findById(eventId).map(this::autoCompleteIfEnded);
    }

    /**
     * Get all pending events
     */
    public List<Event> getPendingEvents() {
        return eventRepository.findPendingEvents();
    }

    /**
     * Get all approved upcoming events
     */
    public List<Event> getUpcomingApprovedEvents() {
        return eventRepository.findUpcomingApprovedEvents();
    }

    /**
     * Get events by status
     */
    public List<Event> getEventsByStatus(EventStatus status) {
        return eventRepository.findByStatus(status);
    }

    /**
     * Get events by club
     */
    public List<Event> getEventsByClub(Long clubId) {
        return eventRepository.findByClubClubId(clubId);
    }

    /**
     * Get events created by specific user (Club Head)
     */
    public List<Event> getEventsByCreator(Long userId) {
        return eventRepository.findByCreatedByUserId(userId)
            .stream()
            .map(this::autoCompleteIfEnded)
            .collect(Collectors.toList());
    }

    private Event autoCompleteIfEnded(Event event) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime effectiveEndDate = event.getEndDate() != null ? event.getEndDate() : event.getEventDate();

        if (event.getStatus() == EventStatus.APPROVED
            && effectiveEndDate != null
            && !effectiveEndDate.isAfter(now)) {
            event.setStatus(EventStatus.COMPLETED);
            log.info(String.format("Event auto-completed after end date: %d", event.getEventId()));
            return eventRepository.save(event);
        }

        return event;
    }

    /**
     * Update event (Club Head)
     */
    public Event updateEvent(Long eventId, String title, String description,
                            LocalDateTime eventDate, String venue, Integer maxCapacity) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        // Only allow updates if event is still pending
        if (!event.getStatus().equals(EventStatus.PENDING)) {
            throw new IllegalArgumentException("Can only update pending events");
        }

        event.setTitle(title);
        event.setDescription(description);
        event.setEventDate(eventDate);
        event.setVenue(venue);
        event.setMaxCapacity(maxCapacity);

        log.info(String.format("Event updated: %d", eventId));
        return eventRepository.save(event);
    }

    /**
     * Complete event
     */
    public Event completeEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        event.setStatus(EventStatus.COMPLETED);
        log.info(String.format("Event marked as completed: %d", eventId));
        return eventRepository.save(event);
    }

    /**
     * Cancel event
     */
    public Event cancelEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        event.setStatus(EventStatus.CANCELLED);
        log.warning(String.format("Event cancelled: %d", eventId));
        return eventRepository.save(event);
    }

    /**
     * Get event count by status
     */
    public long getEventCountByStatus(EventStatus status) {
        return eventRepository.countByStatus(status);
    }

    /**
     * Save event report (Club Head)
     */
    public Event saveEventReport(Long eventId, String report) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        event.setEventReport(report);
        event.setReportSubmittedDate(LocalDateTime.now());
        event.setReportStatus(com.eventra.eventra.enums.ReportStatus.PENDING);
        log.info(String.format("Event report saved for event: %d", eventId));
        return eventRepository.save(event);
    }

    /**
     * Get event report
     */
    public String getEventReport(Long eventId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));
        return event.getEventReport();
    }

    /**
     * Save event (generic method to persist changes)
     */
    public Event updateEvent(Event event) {
        return eventRepository.save(event);
    }
}
