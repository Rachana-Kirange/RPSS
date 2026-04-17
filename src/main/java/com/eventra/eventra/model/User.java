package com.eventra.eventra.model;

import jakarta.persistence.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

import com.eventra.eventra.enums.UserStatus;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_phone", columnList = "phone")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(unique = true, length = 15)
    private String phone;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registrationDate;

    @Column
    private LocalDateTime lastLogin;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus approvalStatus = UserStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column
    private LocalDateTime approvalDate;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column
    private LocalDateTime lastRoleChangeDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_changed_by")
    private User lastRoleChangedBy;

    @Column
    private LocalDateTime clubAssignmentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_club_by")
    private User clubAssignedBy;

    @PrePersist
    protected void onCreate() {
        registrationDate = LocalDateTime.now();
        isActive = true;
    }

    public User() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    // Password encryption/verification methods
    public void encryptPassword(String plainPassword) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        this.password = encoder.encode(plainPassword);
    }

    public boolean isPasswordCorrect(String plainPassword) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.matches(plainPassword, this.password);
    }

    public String getRoleString() {
        return role != null ? role.getRoleName().name() : "UNKNOWN";
    }

    public String getRoleDisplayName() {
        return role != null ? role.getRoleDisplayName() : "Unknown";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UserStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(UserStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(User approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(LocalDateTime approvalDate) {
        this.approvalDate = approvalDate;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getLastRoleChangeDate() {
        return lastRoleChangeDate;
    }

    public void setLastRoleChangeDate(LocalDateTime lastRoleChangeDate) {
        this.lastRoleChangeDate = lastRoleChangeDate;
    }

    public User getLastRoleChangedBy() {
        return lastRoleChangedBy;
    }

    public void setLastRoleChangedBy(User lastRoleChangedBy) {
        this.lastRoleChangedBy = lastRoleChangedBy;
    }

    public LocalDateTime getClubAssignmentDate() {
        return clubAssignmentDate;
    }

    public void setClubAssignmentDate(LocalDateTime clubAssignmentDate) {
        this.clubAssignmentDate = clubAssignmentDate;
    }

    public User getClubAssignedBy() {
        return clubAssignedBy;
    }

    public void setClubAssignedBy(User clubAssignedBy) {
        this.clubAssignedBy = clubAssignedBy;
    }

    @Transient
    public boolean isApproved() {
        return approvalStatus == UserStatus.APPROVED;
    }

    @Transient
    public boolean isPending() {
        return approvalStatus == UserStatus.PENDING;
    }

    @Transient
    public boolean isRejected() {
        return approvalStatus == UserStatus.REJECTED;
    }
}
