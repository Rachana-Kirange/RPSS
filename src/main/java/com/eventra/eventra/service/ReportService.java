package com.eventra.eventra.service;

import com.eventra.eventra.model.Report;
import com.eventra.eventra.model.Event;
import com.eventra.eventra.model.User;
import com.eventra.eventra.repository.ReportRepository;
import com.eventra.eventra.repository.EventRepository;
import com.eventra.eventra.repository.RegistrationRepository;
import com.eventra.eventra.repository.FeedbackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.logging.Logger;

@Service
@Transactional
public class ReportService {

    private static final Logger log = Logger.getLogger(ReportService.class.getName());

    private final ReportRepository reportRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final FeedbackRepository feedbackRepository;

    public ReportService(ReportRepository reportRepository, EventRepository eventRepository,
                         RegistrationRepository registrationRepository, FeedbackRepository feedbackRepository) {
        this.reportRepository = reportRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.feedbackRepository = feedbackRepository;
    }

    /**
     * Generate report for event
     */
    public Report generateEventReport(Long eventId, User generatedBy) {
        log.info(String.format("Generating report for event: %d", eventId));

        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        // Check if report already exists
        Optional<Report> existingReport = reportRepository.findByEventEventId(eventId);
        if (existingReport.isPresent()) {
            return updateEventReport(existingReport.get(), event, generatedBy);
        }

        int totalParticipants = (int) registrationRepository.countByEventEventId(eventId);
        long paidCount = registrationRepository.countPaidRegistrations(eventId);
        Double averageRating = feedbackRepository.getAverageRatingByEvent(eventId);
        long feedbackCount = feedbackRepository.countByEventEventId(eventId);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        if (event.getRequiresPayment()) {
            totalRevenue = event.getPaymentAmount().multiply(BigDecimal.valueOf(paidCount));
        }

        Report report = new Report();
        report.setEvent(event);
        report.setTotalParticipants(totalParticipants);
        report.setTotalAttended(0);
        report.setTotalRevenue(totalRevenue);
        report.setAverageRating(averageRating != null ? BigDecimal.valueOf(averageRating) : null);
        report.setUploadedMediaCount(0);
        report.setFeedbackCount((int) feedbackCount);
        report.setGeneratedBy(generatedBy);

        return reportRepository.save(report);
    }

    /**
     * Update event report (after event completion)
     */
    public Report updateEventReport(Report report, Event event, User generatedBy) {
        int totalAttended = (int) registrationRepository.findConfirmedRegistrationsByEvent(event.getEventId())
            .stream()
            .filter(r -> "ATTENDED".equals(r.getStatus().name()))
            .count();

        Double averageRating = feedbackRepository.getAverageRatingByEvent(event.getEventId());

        report.setTotalAttended(totalAttended);
        if (averageRating != null) {
            report.setAverageRating(BigDecimal.valueOf(averageRating));
        }
        report.setGeneratedBy(generatedBy);

        log.info(String.format("Report updated for event: %d", event.getEventId()));
        return reportRepository.save(report);
    }

    /**
     * Get report by ID
     */
    public Optional<Report> getReportById(Long reportId) {
        return reportRepository.findById(reportId);
    }

    /**
     * Get report for event
     */
    public Optional<Report> getEventReport(Long eventId) {
        return reportRepository.findByEventEventId(eventId);
    }

    /**
     * Update attendance count
     */
    public Report updateAttendanceCount(Long reportId, Integer attendedCount) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("Report not found"));

        report.setTotalAttended(attendedCount);
        log.info(String.format("Attendance count updated for report: %d", reportId));
        return reportRepository.save(report);
    }

    /**
     * Get attendance percentage
     */
    public Double getAttendancePercentage(Long reportId) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("Report not found"));

        return report.getAttendancePercentage();
    }

    /**
     * Check if event was successful
     */
    public Boolean isEventSuccessful(Long reportId) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("Report not found"));

        return report.isSuccessfulEvent();
    }
}
