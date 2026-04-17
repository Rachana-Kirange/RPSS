package com.eventra.eventra.dto;

import jakarta.validation.constraints.*;

public class EventRegistrationDTO {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String studentFullName;

    @NotBlank(message = "Section/Class is required")
    @Size(min = 1, max = 50, message = "Section must be up to 50 characters")
    private String section;

    @NotBlank(message = "Roll number is required")
    @Size(min = 1, max = 20, message = "Roll number must be up to 20 characters")
    private String rollNumber;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobileNumber;

    @NotBlank(message = "Email address is required")
    @Email(message = "Email must be valid")
    private String studentEmail;

    private Long eventId;

    public EventRegistrationDTO() {
    }

    public EventRegistrationDTO(String studentFullName, String section, String rollNumber, 
                                String mobileNumber, String studentEmail, Long eventId) {
        this.studentFullName = studentFullName;
        this.section = section;
        this.rollNumber = rollNumber;
        this.mobileNumber = mobileNumber;
        this.studentEmail = studentEmail;
        this.eventId = eventId;
    }

    public String getStudentFullName() {
        return studentFullName;
    }

    public void setStudentFullName(String studentFullName) {
        this.studentFullName = studentFullName;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }
}
