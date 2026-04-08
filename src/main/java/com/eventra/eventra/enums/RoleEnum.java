package com.eventra.eventra.enums;

public enum RoleEnum {
    PARTICIPANT("Participant - Can register for events and give feedback"),
    CLUB_HEAD("Club Head - Can create and manage events"),
    ADMIN("Admin - System administrator with full access");

    private final String description;

    RoleEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
