package com.eventra.eventra.service;

import com.eventra.eventra.model.User;
import com.eventra.eventra.model.Role;
import com.eventra.eventra.dto.UserRegistrationDTO;
import com.eventra.eventra.dto.UserLoginDTO;
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

@Service
@Transactional
public class UserService {

    private static final Logger log = Logger.getLogger(UserService.class.getName());

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ClubService clubService;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, ClubService clubService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.clubService = clubService;
    }

    /**
     * Register a new user in the system with role selection.
     * Users can register as PARTICIPANT (auto-approved) or CLUB_HEAD (pending approval).
     * ADMIN role can only be assigned by administrators.
     */
    public User registerUser(UserRegistrationDTO dto, RoleEnum selectedRole) {
        log.info(String.format("Registering new user: %s with role: %s", dto.getEmail(), selectedRole));

        if (userRepository.existsByEmail(dto.getEmail())) {
            log.warning(String.format("Registration failed - email already exists: %s", dto.getEmail()));
            throw new IllegalArgumentException("Email already exists");
        }

        // Check if phone already exists
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            if (userRepository.findByPhone(dto.getPhone()).isPresent()) {
                log.warning(String.format("Registration failed - phone already exists: %s", dto.getPhone()));
                throw new IllegalArgumentException("Phone number already registered");
            }
        }

        // Only allow PARTICIPANT and CLUB_HEAD roles during registration
        RoleEnum roleToAssign = selectedRole;
        if (roleToAssign == null || (roleToAssign != RoleEnum.PARTICIPANT && roleToAssign != RoleEnum.CLUB_HEAD)) {
            log.warning(String.format("Registration failed - invalid role attempt: %s for email: %s", roleToAssign, dto.getEmail()));
            throw new IllegalArgumentException("Invalid role selection. Only STUDENT and CLUB_HEAD roles are available during registration.");
        }

        Role role = roleRepository.findByRoleName(roleToAssign)
            .orElseThrow(() -> {
                log.severe(String.format("%s role not found in database. Role initialization required.", roleToAssign));
                return new IllegalArgumentException("System error: Selected role is not available. Please contact administrator.");
            });

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRole(role);
        user.setIsActive(true);

        // APPROVAL STATUS LOGIC:
        // - PARTICIPANT (Students): Auto-approved, can immediately access student dashboard
        // - CLUB_HEAD: Pending approval, must wait for admin review before accessing club head features
        if (roleToAssign == RoleEnum.PARTICIPANT) {
            user.setApprovalStatus(UserStatus.APPROVED); // Auto-approve participants
        } else if (roleToAssign == RoleEnum.CLUB_HEAD) {
            user.setApprovalStatus(UserStatus.PENDING); // Requires admin approval
        }

        // Encrypt password using BCrypt
        user.encryptPassword(dto.getPassword());
        
        // Save user
        User savedUser = userRepository.save(user);
        userRepository.flush();
        
        log.info(String.format("User registered successfully: %s (ID: %d) with role: %s, approval status: %s", 
            savedUser.getEmail(), savedUser.getUserId(), roleToAssign, user.getApprovalStatus()));
        return savedUser;
    }

    /**
     * Register a new user in the system as PARTICIPANT only (Legacy method).
     * Users cannot choose their role during registration - they are always registered as PARTICIPANT.
     * Other roles (CLUB_HEAD, ADMIN) are assigned by administrators only.
     */
    public User registerUser(UserRegistrationDTO dto) {
        // Default to PARTICIPANT role for backward compatibility
        return registerUser(dto, RoleEnum.PARTICIPANT);
    }

    /**
     * Authenticate user with email and password
     */
    public Optional<User> authenticateUser(UserLoginDTO dto) {
        log.fine(String.format("Authenticating user: %s", dto.getEmail()));

        try {
            Optional<User> user = userRepository.findByEmail(dto.getEmail());

            if (user.isPresent()) {
                User foundUser = user.get();
                
                // Check password match
                if (foundUser.isPasswordCorrect(dto.getPassword())) {
                    foundUser.setLastLogin(LocalDateTime.now());
                    User savedUser = userRepository.save(foundUser);
                    
                    // Refresh user from database to ensure role is properly loaded
                    userRepository.flush();
                    Optional<User> refreshedUser = userRepository.findById(savedUser.getUserId());
                    
                    if (refreshedUser.isPresent()) {
                        log.info(String.format("User authenticated successfully: %s", dto.getEmail()));
                        return refreshedUser;
                    }
                }
            }

            log.warning(String.format("Authentication failed for user: %s", dto.getEmail()));
            return Optional.empty();
        } catch (Exception e) {
            log.severe("Exception in authenticateUser: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Get user by ID
     */
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * Get user by email
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Get all active users
     */
    public List<User> getAllActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }

    /**
     * Get all users by role
     */
    public List<User> getUsersByRole(RoleEnum role) {
        return userRepository.findByRole(role);
    }

    /**
     * Update user profile
     */
    public User updateProfile(Long userId, String name, String phone) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(name);
        user.setPhone(phone);
        return userRepository.save(user);
    }

    /**
     * Deactivate user
     */
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(false);
        userRepository.save(user);
        log.info(String.format("User deactivated: %s", user.getEmail()));
    }

    /**
     * Change user password
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isPasswordCorrect(oldPassword)) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.encryptPassword(newPassword);
        userRepository.save(user);
        log.info(String.format("Password changed for user: %s", user.getEmail()));
    }

    // User Approval Workflow Methods
    public void approveUser(Long userId, Long adminId) {
        User user = getUserById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        User admin = getUserById(adminId).orElseThrow(() -> new RuntimeException("Admin not found"));
        
        user.setApprovalStatus(UserStatus.APPROVED);
        user.setApprovedBy(admin);
        user.setApprovalDate(LocalDateTime.now());
        userRepository.save(user);
        
        log.info(String.format("User %s approved by admin %s", user.getEmail(), admin.getEmail()));
    }

    public void rejectUser(Long userId, Long adminId, String reason) {
        User user = getUserById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        User admin = getUserById(adminId).orElseThrow(() -> new RuntimeException("Admin not found"));
        
        user.setApprovalStatus(UserStatus.REJECTED);
        user.setApprovedBy(admin);
        user.setApprovalDate(LocalDateTime.now());
        user.setRejectionReason(reason);
        userRepository.save(user);
        
        log.info(String.format("User %s rejected by admin %s. Reason: %s", user.getEmail(), admin.getEmail(), reason));
    }

    public void suspendUser(Long userId, String reason) {
        User user = getUserById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setApprovalStatus(UserStatus.SUSPENDED);
        user.setRejectionReason(reason);
        userRepository.save(user);
        
        log.info(String.format("User %s suspended. Reason: %s", user.getEmail(), reason));
    }

    public List<User> getPendingUsers() {
        return userRepository.findByApprovalStatus(UserStatus.PENDING);
    }

    public List<User> getApprovedUsers() {
        return userRepository.findByApprovalStatus(UserStatus.APPROVED);
    }

    public List<User> getApprovedUsersByRole(RoleEnum role) {
        return userRepository.findByApprovalStatusAndRole(UserStatus.APPROVED, role);
    }

    public List<User> getRejectedUsers() {
        return userRepository.findByApprovalStatus(UserStatus.REJECTED);
    }

    public UserStatus getApprovalStatus(Long userId) {
        User user = getUserById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return user.getApprovalStatus();
    }

    public boolean isUserApproved(Long userId) {
        User user = getUserById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return user.isApproved();
    }

    public long getPendingUserCount() {
        return userRepository.countByApprovalStatus(UserStatus.PENDING);
    }

    public long getApprovedUserCountByRole(RoleEnum role) {
        return userRepository.countByApprovalStatusAndRole(UserStatus.APPROVED, role);
    }
}
