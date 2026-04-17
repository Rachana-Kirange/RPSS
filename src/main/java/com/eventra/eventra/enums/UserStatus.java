package com.eventra.eventra.enums;

/**
 * User approval status
 * PENDING: Waiting for admin approval
 * APPROVED: Approved by admin - can access dashboard
 * REJECTED: Rejected by admin - cannot access dashboard
 * SUSPENDED: Temporarily suspended
 */
public enum UserStatus {
    PENDING("Pending Approval"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    SUSPENDED("Suspended");

    private final String displayName;

    UserStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
