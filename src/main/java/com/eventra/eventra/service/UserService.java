package com.eventra.eventra.service;

import com.eventra.eventra.model.User;
import com.eventra.eventra.model.Role;
import com.eventra.eventra.dto.UserRegistrationDTO;
import com.eventra.eventra.dto.UserLoginDTO;
import com.eventra.eventra.enums.RoleEnum;
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

    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * Register a new user in the system
     */
    public User registerUser(UserRegistrationDTO dto) {
        log.info(String.format("Registering new user: %s", dto.getEmail()));

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Check if phone already exists
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            if (userRepository.findByPhone(dto.getPhone()).isPresent()) {
                throw new IllegalArgumentException("Phone number already registered");
            }
        }

        Role role = roleRepository.findByRoleName(dto.getRole())
            .orElseThrow(() -> new RuntimeException("Role not found"));

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRole(role);
        user.setIsActive(true);

        user.encryptPassword(dto.getPassword());
        return userRepository.save(user);
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
        return userRepository.findByRole(role.name());
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
}
