package com.eventra.eventra.enums;

public enum PaymentStatus {
    NOT_REQUIRED("Not Required"),
    PENDING("Pending"),
    COMPLETED("Completed"),
    REFUNDED("Refunded");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
