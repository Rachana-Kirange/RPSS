package com.eventra.eventra.enums;

/**
 * Role Hierarchy: ADMIN > CLUB_HEAD > PARTICIPANT
 * 
 * PARTICIPANT: Can register for events and provide feedback
 * CLUB_HEAD: Can create and manage events for their assigned club
 * ADMIN: System administrator with full access to all features
 */
public enum RoleEnum {
    PARTICIPANT("Participant - Can register for events and give feedback", 1),
    CLUB_HEAD("Club Head - Can create and manage events", 2),
    ADMIN("Admin - System administrator with full access", 3);

    private final String description;
    private final int hierarchy; // Higher number = higher privilege

    RoleEnum(String description, int hierarchy) {
        this.description = description;
        this.hierarchy = hierarchy;
    }

    public String getDescription() {
        return description;
    }

    public int getHierarchy() {
        return hierarchy;
    }

    /**
     * Check if this role has higher or equal privilege than another role
     */
    public boolean isHigherOrEqual(RoleEnum other) {
        return this.hierarchy >= other.hierarchy;
    }

    /**
     * Check if this role is higher privilege than another role
     */
    public boolean isHigherThan(RoleEnum other) {
        return this.hierarchy > other.hierarchy;
    }
}
