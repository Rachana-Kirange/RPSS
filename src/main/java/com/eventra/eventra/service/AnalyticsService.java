package com.eventra.eventra.service;

import com.eventra.eventra.dto.AnalyticsDTO;
import com.eventra.eventra.model.Event;
import com.eventra.eventra.model.Registration;
import com.eventra.eventra.model.User;
import com.eventra.eventra.repository.EventRepository;
import com.eventra.eventra.repository.RegistrationRepository;
import com.eventra.eventra.repository.UserRepository;
import com.eventra.eventra.enums.RoleEnum;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    public AnalyticsService(EventRepository eventRepository, RegistrationRepository registrationRepository,
                           UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get overall analytics
     */
    public AnalyticsDTO getOverallAnalytics() {
        AnalyticsDTO analytics = new AnalyticsDTO();

        // Total counts
        analytics.setTotalEvents(eventRepository.count());
        analytics.setTotalRegistrations(registrationRepository.count());
        analytics.setTotalUsers(userRepository.count());
        analytics.setTotalStudents(userRepository.countByRoleName(RoleEnum.PARTICIPANT));
        analytics.setTotalClubHeads(userRepository.countByRoleName(RoleEnum.CLUB_HEAD));

        // Revenue calculations
        BigDecimal totalRevenue = getTotalRevenue();
        analytics.setTotalRevenue(totalRevenue.doubleValue());

        // Get monthly registration data
        analytics.setMonthlyRegistrations(getMonthlyRegistrations());

        // Get event status breakdown
        analytics.setEventsByStatus(getEventStatusBreakdown());

        // Get top events by registration
        analytics.setTopEvents(getTopEventsByRegistration());

        return analytics;
    }

    /**
     * Get monthly registration counts
     */
    private Map<String, Long> getMonthlyRegistrations() {
        List<Registration> registrations = registrationRepository.findAll();
        
        return registrations.stream()
                .collect(Collectors.groupingBy(
                        reg -> reg.getRegistrationDate().getYear() + "-" + 
                               String.format("%02d", reg.getRegistrationDate().getMonthValue()),
                        Collectors.counting()
                ));
    }

    /**
     * Get breakdown of events by status
     */
    private Map<String, Long> getEventStatusBreakdown() {
        List<Event> events = eventRepository.findAll();
        
        return events.stream()
                .collect(Collectors.groupingBy(
                        event -> event.getStatus().name(),
                        Collectors.counting()
                ));
    }

    /**
     * Get top 5 events by registration count
     */
    private List<Map<String, Object>> getTopEventsByRegistration() {
        List<Event> events = eventRepository.findAll();
        
        return events.stream()
                .map(event -> {
                    Map<String, Object> eventData = new HashMap<>();
                    long registrationCount = registrationRepository.countByEvent(event);
                    eventData.put("title", event.getTitle());
                    eventData.put("registrations", registrationCount);
                    eventData.put("eventId", event.getEventId());
                    return eventData;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("registrations"), (Long) a.get("registrations")))
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * Calculate total revenue (assuming registration fees)
     */
    private BigDecimal getTotalRevenue() {
        // Since Payment status is tracked, we can estimate revenue
        // For now, we'll return a placeholder - you can enhance this later
        // If there's a payment amount field, use that instead
        return BigDecimal.ZERO;
    }

    /**
     * Get daily registrations for the last 30 days
     */
    public Map<String, Long> getDailyRegistrationsLastMonth() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Registration> recentRegistrations = registrationRepository.findAll().stream()
                .filter(reg -> reg.getRegistrationDate().isAfter(thirtyDaysAgo))
                .collect(Collectors.toList());

        return recentRegistrations.stream()
                .collect(Collectors.groupingBy(
                        reg -> reg.getRegistrationDate().toLocalDate().toString(),
                        Collectors.counting()
                ));
    }

    /**
     * Get active events count
     */
    public long getActiveEventsCount() {
        return eventRepository.count();
    }

    /**
     * Get user growth data
     */
    public Map<String, Long> getUserGrowthByMonth() {
        List<User> users = userRepository.findAll();
        
        return users.stream()
                .collect(Collectors.groupingBy(
                        user -> user.getRegistrationDate().getYear() + "-" + 
                                String.format("%02d", user.getRegistrationDate().getMonthValue()),
                        Collectors.counting()
                ));
    }
}
