package com.eventra.eventra.controller;

import com.eventra.eventra.model.User;
import com.eventra.eventra.dto.FeedbackSubmitDTO;
import com.eventra.eventra.service.FeedbackService;
import com.eventra.eventra.service.EventService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@Controller
@RequestMapping("/feedback")
public class FeedbackController {

    private static final Logger log = Logger.getLogger(FeedbackController.class.getName());

    private final FeedbackService feedbackService;
    private final EventService eventService;

    public FeedbackController(FeedbackService feedbackService, EventService eventService) {
        this.feedbackService = feedbackService;
        this.eventService = eventService;
    }

    /**
     * Show feedback form
     */
    @GetMapping("/event/{eventId}")
    public String showFeedbackForm(@PathVariable Long eventId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/auth/login";
        }

        var event = eventService.getEventById(eventId);
        if (event.isEmpty()) {
            return "error/not-found";
        }

        if (!feedbackService.canSubmitFeedback(eventId, user.getUserId())) {
            model.addAttribute("error", "You have already submitted feedback for this event");
            return "error/already-submitted";
        }

        model.addAttribute("event", event.get());
        model.addAttribute("feedbackSubmitDTO", new FeedbackSubmitDTO());
        return "feedback/submit-feedback";
    }

    /**
     * Submit feedback
     */
    @PostMapping("/event/{eventId}")
    public String submitFeedback(@PathVariable Long eventId,
                                @Valid @ModelAttribute FeedbackSubmitDTO dto,
                                BindingResult result, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/auth/login";
        }

        if (result.hasErrors()) {
            var event = eventService.getEventById(eventId);
            model.addAttribute("event", event.orElse(null));
            return "feedback/submit-feedback";
        }

        try {
            feedbackService.submitFeedback(eventId, user, dto.getRating(), dto.getComment());
            log.info(String.format("Feedback submitted for event: %d by user: %s", eventId, user.getEmail()));
            return "redirect:/events/" + eventId;
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "feedback/submit-feedback";
        }
    }

    /**
     * View event feedback
     */
    @GetMapping("/event/{eventId}/all")
    public String viewEventFeedback(@PathVariable Long eventId, Model model) {
        var event = eventService.getEventById(eventId);

        if (event.isEmpty()) {
            return "error/not-found";
        }

        var feedbackList = feedbackService.getEventFeedback(eventId);
        Double averageRating = feedbackService.getAverageRating(eventId);

        model.addAttribute("event", event.get());
        model.addAttribute("feedbackList", feedbackList);
        model.addAttribute("averageRating", averageRating != null ? String.format("%.1f", averageRating) : "No ratings");
        model.addAttribute("feedbackCount", feedbackList.size());

        return "feedback/event-feedback";
    }

    /**
     * Edit feedback
     */
    @GetMapping("/{feedbackId}/edit")
    public String showEditFeedbackForm(@PathVariable Long feedbackId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/auth/login";
        }

        var feedback = feedbackService.getFeedbackById(feedbackId);
        if (feedback.isEmpty()) {
            return "error/not-found";
        }

        if (!feedback.get().getParticipant().getUserId().equals(user.getUserId())) {
            return "error/unauthorized";
        }

        FeedbackSubmitDTO dto = new FeedbackSubmitDTO();
        dto.setRating(feedback.get().getRating());
        dto.setComment(feedback.get().getComment());

        model.addAttribute("feedback", feedback.get());
        model.addAttribute("feedbackSubmitDTO", dto);
        return "feedback/edit-feedback";
    }

    /**
     * Update feedback
     */
    @PostMapping("/{feedbackId}/update")
    public String updateFeedback(@PathVariable Long feedbackId,
                                @Valid @ModelAttribute FeedbackSubmitDTO dto,
                                BindingResult result, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/auth/login";
        }

        var feedback = feedbackService.getFeedbackById(feedbackId);
        if (feedback.isEmpty()) {
            return "error/not-found";
        }

        if (!feedback.get().getParticipant().getUserId().equals(user.getUserId())) {
            return "error/unauthorized";
        }

        if (result.hasErrors()) {
            model.addAttribute("feedback", feedback.get());
            return "feedback/edit-feedback";
        }

        feedbackService.updateFeedback(feedbackId, dto.getRating(), dto.getComment());
        log.info(String.format("Feedback updated: %d", feedbackId));
        return "redirect:/events/" + feedback.get().getEvent().getEventId();
    }

    /**
     * Delete feedback
     */
    @PostMapping("/{feedbackId}/delete")
    public String deleteFeedback(@PathVariable Long feedbackId, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/auth/login";
        }

        var feedback = feedbackService.getFeedbackById(feedbackId);
        if (feedback.isEmpty()) {
            return "error/not-found";
        }

        if (!feedback.get().getParticipant().getUserId().equals(user.getUserId())) {
            return "error/unauthorized";
        }

        Long eventId = feedback.get().getEvent().getEventId();
        feedbackService.deleteFeedback(feedbackId);
        log.info(String.format("Feedback deleted: %d", feedbackId));

        return "redirect:/feedback/event/" + eventId + "/all";
    }
}
