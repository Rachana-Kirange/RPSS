package com.eventra.eventra.service;

import com.eventra.eventra.model.User;
import com.eventra.eventra.model.AuditLog;
import com.eventra.eventra.model.Role;
import com.eventra.eventra.dto.UserRegistrationDTO;
import com.eventra.eventra.enums.RoleEnum;
import com.eventra.eventra.enums.UserStatus;
import com.eventra.eventra.repository.UserRepository;
import com.eventra.eventra.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * AdminService handles administrative operations such as:
 * - User management (viewing, activating/deactivating)
 * - Role assignment (PARTICIPANT → CLUB_HEAD, PARTICIPANT/CLUB_HEAD → ADMIN)
 * - Club assignments to CLUB_HEAD users
 * - Audit log retrieval
 * - User approval/rejection
 */
@Service
@Transactional
public class AdminService {

    private static final Logger log = Logger.getLogger(AdminService.class.getName());

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;
    private final ClubService clubService;

    public AdminService(UserRepository userRepository, RoleRepository roleRepository,
                        AuditLogService auditLogService, ClubService clubService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogService = auditLogService;
        this.clubService = clubService;
    }

    /**
     * View all users with pagination capability
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Get users by approval status
     */
    public List<User> getUsersByApprovalStatus(UserStatus status) {
        return userRepository.findByApprovalStatus(status);
    }

    /**
     * Get users by role
     */
    public List<User> getUsersByRole(RoleEnum roleEnum) {
        return userRepository.findByRole(roleEnum);
    }

    /**
     * Activate a user account
     */
    public User activateUser(Long userId, User admin) {
        log.info(String.format("Admin %s attempting to activate user %d", admin.getEmail(), userId));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (user.getIsActive()) {
            log.warning(String.format("User %d is already active", userId));
            throw new IllegalStateException("User is already active");
        }

        user.setIsActive(true);
        User activatedUser = userRepository.save(user);
        
        // Log audit
        logAudit(admin, "USER_ACTIVATION", 
            String.format("User %s (ID: %d) activated", user.getEmail(), userId));
        
        log.info(String.format("User %s (ID: %d) activated by admin %s", 
            user.getEmail(), userId, admin.getEmail()));
        
        return activatedUser;
    }

    /**
     * Deactivate a user account
     */
    public User deactivateUser(Long userId, User admin) {
        log.info(String.format("Admin %s attempting to deactivate user %d", admin.getEmail(), userId));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (!user.getIsActive()) {
            log.warning(String.format("User %d is already inactive", userId));
            throw new IllegalStateException("User is already inactive");
        }

        // Prevent deactivating another admin unless you're a higher-level admin
        if (user.getRole().getRoleName() == RoleEnum.ADMIN && 
            admin.getRole().getRoleName() != RoleEnum.ADMIN) {
            throw new IllegalStateException("Cannot deactivate an admin user");
        }

        user.setIsActive(false);
        User deactivatedUser = userRepository.save(user);
        
        // Log audit
        logAudit(admin, "USER_DEACTIVATION", 
            String.format("User %s (ID: %d) deactivated", user.getEmail(), userId));
        
        log.info(String.format("User %s (ID: %d) deactivated by admin %s", 
            user.getEmail(), userId, admin.getEmail()));
        
        return deactivatedUser;
    }

    /**
     * Assign or upgrade role to a user.
     * Role hierarchy: PARTICIPANT → CLUB_HEAD → ADMIN
     */
    public User assignRole(Long userId, RoleEnum newRole, User admin) {
        log.info(String.format("Admin %s attempting to assign role %s to user %d", 
            admin.getEmail(), newRole, userId));

        // Verify admin has permission
        if (admin.getRole().getRoleName() != RoleEnum.ADMIN) {
            log.warning(String.format("Non-admin user %s attempted to assign role", admin.getEmail()));
            throw new IllegalAccessError("Only admins can assign roles");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        RoleEnum currentRole = user.getRole().getRoleName();

        // Validate role hierarchy: can only upgrade, not downgrade
        if (newRole == currentRole) {
            log.warning(String.format("User %d already has role %s", userId, newRole));
            throw new IllegalArgumentException("User already has the role: " + newRole);
        }

        // Ensure role hierarchy is maintained (higher rank can't be given a lower rank)
        if (newRole.getHierarchy() < currentRole.getHierarchy()) {
            log.warning(String.format("Attempted role downgrade for user %d from %s to %s", 
                userId, currentRole, newRole));
            throw new IllegalStateException("Cannot downgrade user role. Current role: " + currentRole + 
                ", requested role: " + newRole);
        }

        // Prevent non-admin from becoming admin
        if (newRole == RoleEnum.ADMIN && currentRole == RoleEnum.PARTICIPANT) {
            // This is allowed - participant can become club_head first, then admin
            // But we should validate this is intentional
            log.warning(String.format("Direct upgrade of participant to admin. User: %d", userId));
        }

        // Get the new role from database
        Role roleEntity = roleRepository.findByRoleName(newRole)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + newRole));

        String oldRole = currentRole.name();
        user.setRole(roleEntity);
        user.setLastRoleChangeDate(LocalDateTime.now());
        user.setLastRoleChangedBy(admin);

        User updatedUser = userRepository.save(user);
        userRepository.flush();

        // Log audit
        logAudit(admin, "ROLE_ASSIGNMENT", 
            String.format("User %s (ID: %d) role changed from %s to %s", 
                user.getEmail(), userId, oldRole, newRole));

        log.info(String.format("User %s (ID: %d) role changed from %s to %s by admin %s", 
            user.getEmail(), userId, oldRole, newRole, admin.getEmail()));

        return updatedUser;
    }

    /**
     * Approve a pending user registration
     */
    public User approveUser(Long userId, User admin) {
        log.info(String.format("Admin %s approving user %d", admin.getEmail(), userId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (user.getApprovalStatus() == UserStatus.APPROVED) {
            log.warning(String.format("User %d is already approved", userId));
            throw new IllegalStateException("User is already approved");
        }

        user.setApprovalStatus(UserStatus.APPROVED);
        user.setApprovedBy(admin);
        user.setApprovalDate(LocalDateTime.now());
        
        User approvedUser = userRepository.save(user);
        
        // Log audit
        logAudit(admin, "USER_APPROVAL", 
            String.format("User %s (ID: %d) approved", user.getEmail(), userId));
        
        log.info(String.format("User %s (ID: %d) approved by admin %s", 
            user.getEmail(), userId, admin.getEmail()));
        
        return approvedUser;
    }

    /**
     * Reject a pending user registration
     */
    public User rejectUser(Long userId, String reason, User admin) {
        log.info(String.format("Admin %s rejecting user %d", admin.getEmail(), userId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (user.getApprovalStatus() == UserStatus.REJECTED) {
            log.warning(String.format("User %d is already rejected", userId));
            throw new IllegalStateException("User is already rejected");
        }

        user.setApprovalStatus(UserStatus.REJECTED);
        user.setApprovedBy(admin);
        user.setApprovalDate(LocalDateTime.now());
        user.setRejectionReason(reason);
        user.setIsActive(false); // Deactivate rejected users
        
        User rejectedUser = userRepository.save(user);
        
        // Log audit
        logAudit(admin, "USER_REJECTION", 
            String.format("User %s (ID: %d) rejected. Reason: %s", user.getEmail(), userId, reason));
        
        log.info(String.format("User %s (ID: %d) rejected by admin %s. Reason: %s", 
            user.getEmail(), userId, admin.getEmail(), reason));
        
        return rejectedUser;
    }

    /**
     * Get audit logs for a specific user
     */
    public List<AuditLog> getUserAuditLogs(Long userId) {
        return auditLogService.getUserAuditLogs(userId);
    }

    /**
     * Get audit logs by action type
     */
    public List<AuditLog> getAuditLogsByAction(String action) {
        return auditLogService.getAuditLogsByAction(action);
    }

    /**
     * Get recent audit logs
     */
    public List<AuditLog> getRecentAuditLogs(int limit) {
        return auditLogService.getRecentAuditLogs(limit);
    }

    /**
     * Log an audit entry
     */
    private void logAudit(User user, String action, String details) {
        try {
            auditLogService.logAction(user, action, details);
        } catch (Exception e) {
            log.warning(String.format("Failed to log audit entry: %s", e.getMessage()));
        }
    }

    /**
     * Create a user directly (admin-only registration)
     * Can create with any role
     */
    public User createUserAsAdmin(UserRegistrationDTO dto, RoleEnum role, User admin) {
        log.info(String.format("Admin %s creating new user: %s with role: %s", 
            admin.getEmail(), dto.getEmail(), role));

        if (userRepository.existsByEmail(dto.getEmail())) {
            log.warning(String.format("Email already exists: %s", dto.getEmail()));
            throw new IllegalArgumentException("Email already exists");
        }

        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            if (userRepository.findByPhone(dto.getPhone()).isPresent()) {
                log.warning(String.format("Phone already registered: %s", dto.getPhone()));
                throw new IllegalArgumentException("Phone number already registered");
            }
        }

        Role roleEntity = roleRepository.findByRoleName(role)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + role));

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRole(roleEntity);
        user.setIsActive(true);
        user.setApprovalStatus(UserStatus.APPROVED);
        user.setApprovedBy(admin);
        user.setApprovalDate(LocalDateTime.now());

        user.encryptPassword(dto.getPassword());

        User savedUser = userRepository.save(user);
        userRepository.flush();

        // Log audit
        logAudit(admin, "USER_CREATION", 
            String.format("User %s (ID: %d) created with role: %s", 
                savedUser.getEmail(), savedUser.getUserId(), role));

        log.info(String.format("User %s (ID: %d) created by admin %s with role: %s", 
            savedUser.getEmail(), savedUser.getUserId(), admin.getEmail(), role));

        return savedUser;
    }

    /**
     * Get count of users by role
     */
    public long countUsersByRole(RoleEnum role) {
        return userRepository.countByRoleName(role);
    }

    /**
     * Get count of pending approvals
     */
    public long countPendingApprovals() {
        return userRepository.countByApprovalStatus(UserStatus.PENDING);
    }
}
