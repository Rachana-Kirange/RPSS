package com.eventra.eventra.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "passes")
public class Pass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long passId;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "registration_id", nullable = false, unique = true)
    private Registration registration;

    @Column(nullable = false, unique = true, length = 500)
    private String qrCode;

    @Column(length = 500)
    private String qrImagePath;

    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedDate;

    @Column
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private Boolean isScanned = false;

    @Column
    private LocalDateTime scanDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scanned_by")
    private User scannedBy;

    @PrePersist
    protected void onCreate() {
        generatedDate = LocalDateTime.now();
        isScanned = false;
    }

    public Pass() {
    }

    public Long getPassId() {
        return passId;
    }

    public void setPassId(Long passId) {
        this.passId = passId;
    }

    public Registration getRegistration() {
        return registration;
    }

    public void setRegistration(Registration registration) {
        this.registration = registration;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getQrImagePath() {
        return qrImagePath;
    }

    public void setQrImagePath(String qrImagePath) {
        this.qrImagePath = qrImagePath;
    }

    public LocalDateTime getGeneratedDate() {
        return generatedDate;
    }

    public void setGeneratedDate(LocalDateTime generatedDate) {
        this.generatedDate = generatedDate;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Boolean getIsScanned() {
        return isScanned;
    }

    public void setIsScanned(Boolean scanned) {
        isScanned = scanned;
    }

    public LocalDateTime getScanDate() {
        return scanDate;
    }

    public void setScanDate(LocalDateTime scanDate) {
        this.scanDate = scanDate;
    }

    public User getScannedBy() {
        return scannedBy;
    }

    public void setScannedBy(User scannedBy) {
        this.scannedBy = scannedBy;
    }

    public void markAsScanned(User scanner) {
        this.isScanned = true;
        this.scanDate = LocalDateTime.now();
        this.scannedBy = scanner;
    }

    public Boolean isExpired() {
        if (expiryDate == null) return false;
        return LocalDateTime.now().isAfter(expiryDate);
    }

    public Boolean isValidForScanning() {
        return !isScanned && !isExpired();
    }
}
