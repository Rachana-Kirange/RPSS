package com.eventra.eventra.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    @Column(nullable = false)
    private Integer totalParticipants = 0;

    @Column(nullable = false)
    private Integer totalAttended = 0;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(nullable = false)
    private Integer uploadedMediaCount = 0;

    @Column(nullable = false)
    private Integer feedbackCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(length = 500)
    private String reportPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by")
    private User generatedBy;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
    }

    public Report() {
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Integer getTotalParticipants() {
        return totalParticipants;
    }

    public void setTotalParticipants(Integer totalParticipants) {
        this.totalParticipants = totalParticipants;
    }

    public Integer getTotalAttended() {
        return totalAttended;
    }

    public void setTotalAttended(Integer totalAttended) {
        this.totalAttended = totalAttended;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getUploadedMediaCount() {
        return uploadedMediaCount;
    }

    public void setUploadedMediaCount(Integer uploadedMediaCount) {
        this.uploadedMediaCount = uploadedMediaCount;
    }

    public Integer getFeedbackCount() {
        return feedbackCount;
    }

    public void setFeedbackCount(Integer feedbackCount) {
        this.feedbackCount = feedbackCount;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getReportPath() {
        return reportPath;
    }

    public void setReportPath(String reportPath) {
        this.reportPath = reportPath;
    }

    public User getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(User generatedBy) {
        this.generatedBy = generatedBy;
    }

    public Double getAttendancePercentage() {
        if (totalParticipants == 0) return 0.0;
        return (totalAttended * 100.0) / totalParticipants;
    }

    public Boolean isSuccessfulEvent() {
        if (averageRating == null) return false;
        return getAttendancePercentage() >= 60.0 && averageRating.doubleValue() >= 3.5;
    }
}
