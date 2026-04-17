package com.eventra.eventra.enums;

/**
 * Enumeration for report approval status
 */
public enum ReportStatus {
    PENDING("Pending Review"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    REVISIONS_NEEDED("Revisions Needed");

    private final String displayName;

    ReportStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
