package com.eventra.eventra.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for admin user rejection operations
 */
public class UserRejectionDTO {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Rejection reason is required")
    private String reason;

    public UserRejectionDTO() {
    }

    public UserRejectionDTO(Long userId, String reason) {
        this.userId = userId;
        this.reason = reason;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
