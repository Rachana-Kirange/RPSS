package com.eventra.eventra.enums;

public enum MediaFileType {
    IMAGE("Image"),
    VIDEO("Video"),
    DOCUMENT("Document");

    private final String label;

    MediaFileType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
