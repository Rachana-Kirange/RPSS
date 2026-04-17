package com.eventra.eventra.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Audit log entity to track all user actions for security and compliance
 * Logs: login, logout, role changes, user activation/deactivation, event creation, etc.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_timestamp", columnList = "created_at"),
    @Index(name = "idx_action", columnList = "action"),
    @Index(name = "idx_user_action", columnList = "user_id,action")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auditLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String action; // e.g., LOGIN, LOGOUT, ROLE_CHANGE, USER_ACTIVATION, EVENT_CREATION

    @Column(columnDefinition = "TEXT")
    private String details; // Additional details about the action

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(length = 45)
    private String ipAddress; // IPv4 or IPv6

    @Column(length = 255)
    private String userAgent;

    @Column(length = 50)
    private String status; // SUCCESS, FAILURE, PARTIAL_SUCCESS

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public AuditLog() {
    }

    public AuditLog(User user, String action, String details) {
        this.user = user;
        this.action = action;
        this.details = details;
        this.status = "SUCCESS";
    }

    public AuditLog(User user, String action, String details, String ipAddress, String userAgent) {
        this.user = user;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.status = "SUCCESS";
    }

    // Getters and Setters
    public Long getAuditLogId() {
        return auditLogId;
    }

    public void setAuditLogId(Long auditLogId) {
        this.auditLogId = auditLogId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
