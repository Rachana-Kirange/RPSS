package com.eventra.eventra.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class EventCreateDTO {

    @NotBlank(message = "Event title is required")
    @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    @NotBlank(message = "Event date and time is required")
    private String eventDateTime; // Format: yyyy-MM-dd'T'HH:mm

    @NotBlank(message = "Venue is required")
    @Size(min = 3, max = 200, message = "Venue must be between 3 and 200 characters")
    private String venue;

    @NotNull(message = "Max capacity is required")
    @Min(value = 5, message = "Minimum capacity must be 5")
    @Max(value = 5000, message = "Maximum capacity cannot exceed 5000")
    private Integer maxCapacity;

    private Boolean requiresPayment = false;

    @DecimalMin(value = "0.0", inclusive = false, message = "Payment amount must be greater than 0")
    @DecimalMax(value = "100000.0", message = "Payment amount cannot exceed 100000")
    private BigDecimal paymentAmount;

    private Boolean requiresQR = false;

    @Size(min = 10, max = 2000, message = "Activity proposal must be between 10 and 2000 characters")
    private String activityProposal;

    public EventCreateDTO() {
    }

    public EventCreateDTO(String title, String description, String eventDateTime, String venue,
                          Integer maxCapacity, Boolean requiresPayment, BigDecimal paymentAmount,
                          Boolean requiresQR, String activityProposal) {
        this.title = title;
        this.description = description;
        this.eventDateTime = eventDateTime;
        this.venue = venue;
        this.maxCapacity = maxCapacity;
        this.requiresPayment = requiresPayment;
        this.paymentAmount = paymentAmount;
        this.requiresQR = requiresQR;
        this.activityProposal = activityProposal;
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

    public String getEventDateTime() {
        return eventDateTime;
    }

    public void setEventDateTime(String eventDateTime) {
        this.eventDateTime = eventDateTime;
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

    public Boolean getRequiresPayment() {
        return requiresPayment;
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
}
