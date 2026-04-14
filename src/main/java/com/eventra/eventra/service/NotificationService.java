package com.eventra.eventra.service;

import com.eventra.eventra.model.Registration;
import com.eventra.eventra.model.Event;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Service for sending notifications to participants
 */
@Service
public class NotificationService {

    private static final Logger log = Logger.getLogger(NotificationService.class.getName());

    /**
     * Send event registration confirmation notification
     */
    public void sendRegistrationConfirmation(Registration registration) {
        Event event = registration.getEvent();
        String participantEmail = registration.getParticipantEmail();
        String mobileNumber = registration.getMobileNumber();

        try {
            // Send Email
            sendRegistrationEmail(registration, event, participantEmail);

            // Send SMS
            if (mobileNumber != null && !mobileNumber.isEmpty()) {
                sendRegistrationSMS(registration, event, mobileNumber);
            }

            log.info(String.format("Notification sent to %s for event registration", participantEmail));
        } catch (Exception e) {
            log.severe("Error sending notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send registration email
     */
    private void sendRegistrationEmail(Registration registration, Event event, String email) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            String eventDate = event.getEventDate().format(formatter);

            String subject = "Event Registration Confirmation - " + event.getTitle();
            String body = String.format(
                "Dear %s,\n\n" +
                "You have successfully registered for the event: %s\n\n" +
                "Event Details:\n" +
                "- Title: %s\n" +
                "- Date & Time: %s\n" +
                "- Venue: %s\n" +
                "- Section: %s\n" +
                "- Roll Number: %s\n\n" +
                "Your Registration ID: %d\n" +
                "Payment Status: %s\n\n" +
                "Please download your pass from the dashboard.\n\n" +
                "Thank you for registering!\n\n" +
                "Regards,\nEventra Event Management System",
                registration.getParticipantFullName(),
                event.getTitle(),
                event.getTitle(),
                eventDate,
                event.getVenue(),
                registration.getSection(),
                registration.getRollNumber(),
                registration.getRegistrationId(),
                registration.getPaymentStatus()
            );

            // TODO: Integrate with email service (JavaMailSender, SendGrid, etc.)
            log.info(String.format("Email would be sent to: %s with subject: %s", email, subject));
            
            // For now, just log it
            System.out.println("📧 EMAIL TO: " + email);
            System.out.println("SUBJECT: " + subject);
            System.out.println("BODY:\n" + body);

        } catch (Exception e) {
            log.severe("Error sending email: " + e.getMessage());
        }
    }

    /**
     * Send registration SMS
     */
    private void sendRegistrationSMS(Registration registration, Event event, String phoneNumber) {
        try {
            String message = String.format(
                "Hi %s! You are registered for %s on %s at %s. Registration ID: %d. Ref: Eventra",
                registration.getParticipantFullName(),
                event.getTitle(),
                event.getEventDate().toLocalDate(),
                event.getVenue(),
                registration.getRegistrationId()
            );

            // TODO: Integrate with SMS service (Twilio, AWS SNS, etc.)
            log.info(String.format("SMS would be sent to: %s", phoneNumber));
            
            // For now, just log it
            System.out.println("📱 SMS TO: " + phoneNumber);
            System.out.println("MESSAGE: " + message);

        } catch (Exception e) {
            log.severe("Error sending SMS: " + e.getMessage());
        }
    }
}
