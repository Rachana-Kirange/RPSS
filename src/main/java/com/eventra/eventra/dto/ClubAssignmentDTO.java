package com.eventra.eventra.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for admin club assignment operations
 */
public class ClubAssignmentDTO {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Club ID is required")
    private Long clubId;

    private String reason; // Optional reason for club assignment

    public ClubAssignmentDTO() {
    }

    public ClubAssignmentDTO(Long userId, Long clubId) {
        this.userId = userId;
        this.clubId = clubId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getClubId() {
        return clubId;
    }

    public void setClubId(Long clubId) {
        this.clubId = clubId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
