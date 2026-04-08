package com.eventra.eventra.enums;

public enum RegistrationStatus {
    CONFIRMED("Confirmed"),
    CANCELLED("Cancelled"),
    ATTENDED("Attended");

    private final String label;

    RegistrationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
