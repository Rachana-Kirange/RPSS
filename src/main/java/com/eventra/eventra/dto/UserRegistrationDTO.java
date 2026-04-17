package com.eventra.eventra.dto;

import com.eventra.eventra.enums.RoleEnum;
import jakarta.validation.constraints.*;

/**
 * DTO for user registration.
 * Users can register as PARTICIPANT (Student) or CLUB_HEAD.
 * ADMIN role is assigned by administrators only.
 * 
 * Registration Flow:
 * - PARTICIPANT users: Auto-approved, immediately see student dashboard
 * - CLUB_HEAD users: Pending approval, see waiting-approval page until admin approves
 */
public class UserRegistrationDTO {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters for security")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
        message = "Password must contain at least one letter, one number, and one special character (@$!%*#?&)"
    )
    private String password;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    private String confirmPassword;

    // Role is optional here - it's set separately by admin during user creation
    // Regular users can't select role; admins can assign any role
    private RoleEnum role;

    public UserRegistrationDTO() {
    }

    public UserRegistrationDTO(String name, String email, String password, String phone, String confirmPassword) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.confirmPassword = confirmPassword;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    /**
     * Get the default role for new registrations.
     * All new users register as PARTICIPANT.
     */
    public RoleEnum getDefaultRole() {
        return RoleEnum.PARTICIPANT;
    }

    public RoleEnum getRole() {
        return role;
    }

    public void setRole(RoleEnum role) {
        // Allow all roles - admin can assign any role when creating users via admin panel
        this.role = role != null ? role : RoleEnum.PARTICIPANT;
    }

    private boolean isValid() {
        return password != null && password.equals(confirmPassword);
    }
}
