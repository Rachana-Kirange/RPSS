package com.eventra.eventra.service;

import com.eventra.eventra.model.Event;
import com.eventra.eventra.model.Registration;
import com.eventra.eventra.model.User;
import com.eventra.eventra.enums.RegistrationStatus;
import com.eventra.eventra.enums.PaymentStatus;
import com.eventra.eventra.repository.RegistrationRepository;
import com.eventra.eventra.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
@Transactional
public class RegistrationService {

    private static final Logger log = Logger.getLogger(RegistrationService.class.getName());

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;

    public RegistrationService(RegistrationRepository registrationRepository, EventRepository eventRepository) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * Register participant for event
     */
    public Registration registerForEvent(Long eventId, User participant) {
        log.info(String.format("Registering user %s for event %d", participant.getEmail(), eventId));

        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        // Check if user already registered
        if (registrationRepository.existsByEventAndParticipant(event, participant)) {
            throw new IllegalArgumentException("User already registered for this event");
        }

        // Check if seats available
        if (!event.isSpaceAvailable()) {
            throw new IllegalArgumentException("Event is full, no seats available");
        }

        Registration registration = new Registration();
        registration.setEvent(event);
        registration.setParticipant(participant);
        registration.setStatus(RegistrationStatus.CONFIRMED);

        // Automatically set payment status
        if (!event.getRequiresPayment()) {
            registration.setPaymentStatus(PaymentStatus.NOT_REQUIRED);
        } else {
            registration.setPaymentStatus(PaymentStatus.PENDING);
        }

        return registrationRepository.save(registration);
    }

    /**
     * Cancel registration
     */
    public void cancelRegistration(Long registrationId) {
        Registration registration = registrationRepository.findById(registrationId)
            .orElseThrow(() -> new RuntimeException("Registration not found"));

        registration.setStatus(RegistrationStatus.CANCELLED);
        registrationRepository.save(registration);
        log.info(String.format("Registration cancelled: %d", registrationId));
    }

    /**
     * Mark attendance
     */
    public void markAttendance(Long registrationId) {
        Registration registration = registrationRepository.findById(registrationId)
            .orElseThrow(() -> new RuntimeException("Registration not found"));

        registration.markAsAttended();
        registrationRepository.save(registration);
        log.info(String.format("Attendance marked for registration: %d", registrationId));
    }

    /**
     * Complete payment
     */
    public void completePayment(Long registrationId, String transactionId) {
        Registration registration = registrationRepository.findById(registrationId)
            .orElseThrow(() -> new RuntimeException("Registration not found"));

        registration.completePayment(transactionId);
        registrationRepository.save(registration);
        log.info(String.format("Payment completed for registration %d: %s", registrationId, transactionId));
    }

    /**
     * Get registration by ID
     */
    public Optional<Registration> getRegistrationById(Long registrationId) {
        return registrationRepository.findById(registrationId);
    }

    /**
     * Get registrations for an event
     */
    public List<Registration> getEventRegistrations(Long eventId) {
        return registrationRepository.findByEventEventId(eventId);
    }

    /**
     * Get participant's registrations
     */
    public List<Registration> getParticipantRegistrations(Long userId) {
        return registrationRepository.findByParticipantUserId(userId);
    }

    /**
     * Get confirmed registrations count
     */
    public long getConfirmedRegistrationCount(Long eventId) {
        return registrationRepository.findConfirmedRegistrationsByEvent(eventId).size();
    }

    /**
     * Get paid registrations count
     */
    public long getPaidRegistrationCount(Long eventId) {
        return registrationRepository.countPaidRegistrations(eventId);
    }

    /**
     * Check if user is registered for event
     */
    public boolean isUserRegistered(Long eventId, Long userId) {
        List<Registration> registrations = registrationRepository.findByEventEventId(eventId);
        return registrations.stream()
            .anyMatch(reg -> reg.getParticipant().getUserId().equals(userId) &&
                           reg.getStatus() == RegistrationStatus.CONFIRMED);
    }

    /**
     * Get registration for user and event
     */
    public Optional<Registration> getUserEventRegistration(Long eventId, Long userId) {
        List<Registration> registrations = registrationRepository.findByEventEventId(eventId);
        return registrations.stream()
            .filter(reg -> reg.getParticipant().getUserId().equals(userId))
            .findFirst();
    }
}
