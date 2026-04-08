package com.eventra.eventra.model;

import com.eventra.eventra.enums.MediaFileType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "media", indexes = {
    @Index(name = "idx_media_event", columnList = "event_id")
})
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mediaId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(nullable = false, length = 500)
    private String filePath;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaFileType fileType;

    @Column
    private Long fileSize;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadDate;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean isApproved = true;

    @PrePersist
    protected void onCreate() {
        uploadDate = LocalDateTime.now();
        isApproved = true;
    }

    public Media() {
    }

    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long mediaId) {
        this.mediaId = mediaId;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(User uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public MediaFileType getFileType() {
        return fileType;
    }

    public void setFileType(MediaFileType fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsApproved() {
        return isApproved;
    }

    public void setIsApproved(Boolean approved) {
        isApproved = approved;
    }

    public Boolean isImage() {
        return fileType == MediaFileType.IMAGE;
    }

    public Boolean isVideo() {
        return fileType == MediaFileType.VIDEO;
    }

    public String getFileSizeInMB() {
        if (fileSize == null) return "0 MB";
        double mb = fileSize / (1024.0 * 1024.0);
        return String.format("%.2f MB", mb);
    }
}
