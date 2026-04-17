package com.eventra.eventra.dto;

import com.eventra.eventra.enums.RoleEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for admin role assignment operations
 */
public class RoleAssignmentDTO {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Role is required")
    private RoleEnum newRole;

    private String reason; // Optional reason for role change

    public RoleAssignmentDTO() {
    }

    public RoleAssignmentDTO(Long userId, RoleEnum newRole) {
        this.userId = userId;
        this.newRole = newRole;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public RoleEnum getNewRole() {
        return newRole;
    }

    public void setNewRole(RoleEnum newRole) {
        this.newRole = newRole;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
