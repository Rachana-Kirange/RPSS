package com.eventra.eventra.service;

import com.eventra.eventra.model.AuditLog;
import com.eventra.eventra.model.User;
import com.eventra.eventra.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service for managing audit logs
 * Tracks all user actions: login, logout, role changes, user activation/deactivation, event creation, etc.
 */
@Service
@Transactional
public class AuditLogService {

    private static final Logger log = Logger.getLogger(AuditLogService.class.getName());

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Log user action with basic information
     */
    public AuditLog logAction(User user, String action, String details) {
        AuditLog auditLog = new AuditLog(user, action, details);
        return auditLogRepository.save(auditLog);
    }

    /**
     * Log user action with IP address and user agent
     */
    public AuditLog logAction(User user, String action, String details, String ipAddress, String userAgent) {
        AuditLog auditLog = new AuditLog(user, action, details, ipAddress, userAgent);
        return auditLogRepository.save(auditLog);
    }

    /**
     * Log user login
     */
    public AuditLog logLogin(User user, String ipAddress, String userAgent) {
        String details = String.format("User logged in with role: %s", user.getRole().getRoleDisplayName());
        return logAction(user, "LOGIN", details, ipAddress, userAgent);
    }

    /**
     * Log user logout
     */
    public AuditLog logLogout(User user, String ipAddress, String userAgent) {
        return logAction(user, "LOGOUT", "User logged out", ipAddress, userAgent);
    }

    /**
     * Log role change
     */
    public AuditLog logRoleChange(User user, String oldRole, String newRole, User changedBy) {
        String details = String.format("Role changed from %s to %s by %s", oldRole, newRole, changedBy.getName());
        AuditLog auditLog = new AuditLog(user, "ROLE_CHANGE", details);
        auditLog.setStatus("SUCCESS");
        return auditLogRepository.save(auditLog);
    }

    /**
     * Log user activation
     */
    public AuditLog logUserActivation(User user, User activatedBy) {
        String details = String.format("User activated by %s", activatedBy.getName());
        AuditLog auditLog = new AuditLog(user, "USER_ACTIVATION", details);
        auditLog.setStatus("SUCCESS");
        return auditLogRepository.save(auditLog);
    }

    /**
     * Log user deactivation
     */
    public AuditLog logUserDeactivation(User user, User deactivatedBy) {
        String details = String.format("User deactivated by %s", deactivatedBy.getName());
        AuditLog auditLog = new AuditLog(user, "USER_DEACTIVATION", details);
        auditLog.setStatus("SUCCESS");
        return auditLogRepository.save(auditLog);
    }

    /**
     * Log club assignment
     */
    public AuditLog logClubAssignment(User user, String clubName, User assignedBy) {
        String details = String.format("Club '%s' assigned by %s", clubName, assignedBy.getName());
        AuditLog auditLog = new AuditLog(user, "CLUB_ASSIGNMENT", details);
        auditLog.setStatus("SUCCESS");
        return auditLogRepository.save(auditLog);
    }

    /**
     * Log event creation
     */
    public AuditLog logEventCreation(User user, String eventName, Long eventId) {
        String details = String.format("Event created: %s (ID: %d)", eventName, eventId);
        AuditLog auditLog = new AuditLog(user, "EVENT_CREATION", details);
        auditLog.setStatus("SUCCESS");
        return auditLogRepository.save(auditLog);
    }

    /**
     * Log failed action
     */
    public AuditLog logFailed(User user, String action, String details, String errorMessage) {
        AuditLog auditLog = new AuditLog(user, action, details);
        auditLog.setStatus("FAILURE");
        auditLog.setErrorMessage(errorMessage);
        return auditLogRepository.save(auditLog);
    }

    /**
     * Get audit logs for a user
     */
    public List<AuditLog> getUserAuditLogs(Long userId) {
        return auditLogRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get audit logs for a specific action
     */
    public List<AuditLog> getAuditLogsByAction(String action) {
        return auditLogRepository.findByActionOrderByCreatedAtDesc(action);
    }

    /**
     * Get audit logs within a date range for a specific user
     */
    public List<AuditLog> getUserAuditLogsByDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findByUserAndDateRange(userId, startDate, endDate);
    }

    /**
     * Get audit logs by action within a date range
     */
    public List<AuditLog> getAuditLogsByActionAndDateRange(String action, LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findByActionAndDateRange(action, startDate, endDate);
    }

    /**
     * Get audit logs by status (SUCCESS, FAILURE, PARTIAL_SUCCESS)
     */
    public List<AuditLog> getAuditLogsByStatus(String status) {
        return auditLogRepository.findByStatus(status);
    }

    /**
     * Get recent audit logs
     */
    public List<AuditLog> getRecentAuditLogs(int limit) {
        return auditLogRepository.findRecentAuditLogs(limit);
    }

    /**
     * Get all audit logs (for admin dashboard)
     */
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
    }
}
