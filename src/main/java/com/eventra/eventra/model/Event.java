package com.eventra.eventra.model;

import com.eventra.eventra.enums.EventStatus;
import com.eventra.eventra.enums.ReportStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_event_status", columnList = "status"),
    @Index(name = "idx_event_date", columnList = "event_date"),
    @Index(name = "idx_event_club", columnList = "club_id")
})
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime eventDate;

    @Column
    private LocalDateTime endDate;

    @Column(nullable = false, length = 200)
    private String venue;

    @Column(nullable = false)
    private Integer maxCapacity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.PENDING;

    @Column(nullable = false)
    private Boolean requiresPayment = false;

    @Column(precision = 10, scale = 2)
    private BigDecimal paymentAmount;

    @Column(nullable = false)
    private Boolean requiresQR = false;

    @Column(columnDefinition = "TEXT")
    private String activityProposal;

    @Column(columnDefinition = "TEXT")
    private String eventReport;

    @Column
    private LocalDateTime reportSubmittedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private ReportStatus reportStatus = ReportStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_reviewed_by")
    private User reportReviewedBy;

    @Column(columnDefinition = "TEXT")
    private String reportReviewComments;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column
    private LocalDateTime approvalDate;

    @Column(length = 500)
    private String rejectionReason;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Registration> registrations = new HashSet<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Feedback> feedbacks = new HashSet<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Media> media = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        status = EventStatus.PENDING;
    }

    public Event() {
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public Club getClub() {
        return club;
    }

    public void setClub(Club club) {
        this.club = club;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public void setRequiresPayment(Boolean requiresPayment) {
        this.requiresPayment = requiresPayment;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(User approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(LocalDateTime approvalDate) {
        this.approvalDate = approvalDate;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Set<Registration> getRegistrations() {
        return registrations;
    }

    public void setRegistrations(Set<Registration> registrations) {
        this.registrations = registrations;
    }

    public Set<Feedback> getFeedbacks() {
        return feedbacks;
    }

    public void setFeedbacks(Set<Feedback> feedbacks) {
        this.feedbacks = feedbacks;
    }

    public Set<Media> getMedia() {
        return media;
    }

    public void setMedia(Set<Media> media) {
        this.media = media;
    }

    @Transient
    public Integer getAvailableSeats() {
        int registeredCount = registrations != null ? registrations.size() : 0;
        return maxCapacity - registeredCount;
    }

    @Transient
    public Boolean isSpaceAvailable() {
        return getAvailableSeats() > 0;
    }

    @Transient
    public Boolean isFreeEvent() {
        return !requiresPayment;
    }

    @Transient
    public int getRegistrationCount() {
        return registrations != null ?  registrations.size() : 0;
    }

    public void approveEvent(User admin) {
        this.status = EventStatus.APPROVED;
        this.approvedBy = admin;
        this.approvalDate = LocalDateTime.now();
    }

    public void rejectEvent(User admin, String reason) {
        this.status = EventStatus.REJECTED;
        this.approvedBy = admin;
        this.approvalDate = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    public String getStatusBadgeClass() {
        return switch (status) {
            case PENDING -> "badge-warning";
            case APPROVED -> "badge-success";
            case REJECTED -> "badge-danger";
            case COMPLETED -> "badge-info";
            case CANCELLED -> "badge-secondary";
            default -> "badge-light";
        };
    }

    public Boolean getRequiresPayment() {
        return requiresPayment;
    }

    public Boolean getRequiresQR() {
        return requiresQR;
    }

    public void setRequiresQR(Boolean requiresQR) {
        this.requiresQR = requiresQR;
    }

    public String getActivityProposal() {
        return activityProposal;
    }

    public void setActivityProposal(String activityProposal) {
        this.activityProposal = activityProposal;
    }

    public String getEventReport() {
        return eventReport;
    }

    public void setEventReport(String eventReport) {
        this.eventReport = eventReport;
    }

    public LocalDateTime getReportSubmittedDate() {
        return reportSubmittedDate;
    }

    public void setReportSubmittedDate(LocalDateTime reportSubmittedDate) {
        this.reportSubmittedDate = reportSubmittedDate;
    }

    public ReportStatus getReportStatus() {
        return reportStatus;
    }

    public void setReportStatus(ReportStatus reportStatus) {
        this.reportStatus = reportStatus;
    }

    public User getReportReviewedBy() {
        return reportReviewedBy;
    }

    public void setReportReviewedBy(User reportReviewedBy) {
        this.reportReviewedBy = reportReviewedBy;
    }

    public String getReportReviewComments() {
        return reportReviewComments;
    }

    public void setReportReviewComments(String reportReviewComments) {
        this.reportReviewComments = reportReviewComments;
    }
}
