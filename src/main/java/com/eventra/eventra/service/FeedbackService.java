package com.eventra.eventra.service;

import com.eventra.eventra.model.Feedback;
import com.eventra.eventra.model.Event;
import com.eventra.eventra.model.User;
import com.eventra.eventra.repository.FeedbackRepository;
import com.eventra.eventra.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
@Transactional
public class FeedbackService {

    private static final Logger log = Logger.getLogger(FeedbackService.class.getName());

    private final FeedbackRepository feedbackRepository;
    private final EventRepository eventRepository;

    public FeedbackService(FeedbackRepository feedbackRepository, EventRepository eventRepository) {
        this.feedbackRepository = feedbackRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * Submit feedback for event
     */
    public Feedback submitFeedback(Long eventId, User student, Integer rating, String comment) {
        log.info(String.format("Submitting feedback for event: %d by user: %s", eventId, student.getEmail()));

        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        // Check if feedback already exists
        Optional<Feedback> existingFeedback = feedbackRepository.findByEventAndStudent(event, student);
        if (existingFeedback.isPresent()) {
            throw new IllegalArgumentException("Feedback already submitted for this event");
        }

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        Feedback feedback = new Feedback();
        feedback.setEvent(event);
        feedback.setStudent(student);
        feedback.setRating(rating);
        feedback.setComment(comment);

        return feedbackRepository.save(feedback);
    }

    /**
     * Update feedback
     */
    public Feedback updateFeedback(Long feedbackId, Integer rating, String comment) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow(() -> new RuntimeException("Feedback not found"));

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        feedback.setRating(rating);
        feedback.setComment(comment);

        log.info(String.format("Feedback updated: %d", feedbackId));
        return feedbackRepository.save(feedback);
    }

    /**
     * Get feedback by ID
     */
    public Optional<Feedback> getFeedbackById(Long feedbackId) {
        return feedbackRepository.findById(feedbackId);
    }

    /**
     * Get all feedback for event
     */
    public List<Feedback> getEventFeedback(Long eventId) {
        return feedbackRepository.findByEventEventId(eventId);
    }

    /**
     * Get average rating for event
     */
    public Double getAverageRating(Long eventId) {
        return feedbackRepository.getAverageRatingByEvent(eventId);
    }

    /**
     * Get feedback count for event
     */
    public long getFeedbackCount(Long eventId) {
        return feedbackRepository.countByEventEventId(eventId);
    }

    /**
     * Delete feedback
     */
    public void deleteFeedback(Long feedbackId) {
        feedbackRepository.deleteById(feedbackId);
        log.info(String.format("Feedback deleted: %d", feedbackId));
    }

    /**
     * Check if user can submit feedback
     */
    public boolean canSubmitFeedback(Long eventId, Long userId) {
        Optional<Feedback> existing = feedbackRepository.findByEventEventId(eventId)
            .stream()
            .filter(f -> f.getStudent().getUserId().equals(userId))
            .findFirst();

        return existing.isEmpty();
    }

    /**
     * Get average rating summary
     */
    public String getAverageRatingSummary(Long eventId) {
        Double avg = getAverageRating(eventId);
        if (avg == null) return "No ratings yet";
        return String.format("%.1f / 5.0", avg);
    }
}
