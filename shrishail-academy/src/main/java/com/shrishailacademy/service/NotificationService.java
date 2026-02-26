package com.shrishailacademy.service;

import com.shrishailacademy.model.Enrollment;
import com.shrishailacademy.model.Payment;
import com.shrishailacademy.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Notification Service - Handles WhatsApp and other notifications.
 * 
 * Currently implements logging-based notifications.
 * Ready for Twilio/Meta WhatsApp Business API integration.
 * 
 * To enable real WhatsApp:
 * 1. Add twilio-sdk dependency to pom.xml
 * 2. Set whatsapp.enabled=true in application.properties
 * 3. Configure whatsapp.api.key and whatsapp.api.url
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Value("${whatsapp.enabled:false}")
    private boolean whatsappEnabled;

    @Value("${whatsapp.api.url:}")
    private String whatsappApiUrl;

    @Value("${whatsapp.api.key:}")
    private String whatsappApiKey;

    @Value("${whatsapp.sender.number:}")
    private String senderNumber;

    /**
     * Send enrollment confirmation via WhatsApp
     */
    public void sendEnrollmentConfirmation(Enrollment enrollment) {
        User student = enrollment.getUser();
        String message = String.format(
                "🎓 *BrightNest Academy*\n\n" +
                        "Hi %s! Welcome aboard! 🎉\n\n" +
                        "You have been successfully enrolled in:\n" +
                        "📚 *%s*\n" +
                        "⏱ Duration: %s\n\n" +
                        "We look forward to an exciting learning journey with you!\n\n" +
                        "📞 Contact: +91-7204193980\n" +
                        "📧 info@brightnest-academy.com",
                student.getName(),
                enrollment.getCourse().getTitle(),
                enrollment.getCourse().getDuration());

        sendWhatsAppMessage(student.getPhone(), message, "ENROLLMENT_CONFIRMATION");
    }

    /**
     * Send payment confirmation via WhatsApp
     */
    public void sendPaymentConfirmation(Payment payment) {
        User student = payment.getUser();
        String message = String.format(
                "🎓 *BrightNest Academy*\n\n" +
                        "Hi %s! Payment Confirmed! ✅\n\n" +
                        "💰 Amount: ₹%.0f\n" +
                        "📚 Course: %s\n" +
                        "🧾 Receipt: %s\n" +
                        "💳 Method: %s\n" +
                        "📅 Date: %s\n\n" +
                        "Thank you for your payment!\n\n" +
                        "📞 Contact: +91-7204193980",
                student.getName(),
                payment.getAmount(),
                payment.getCourse().getTitle(),
                payment.getReceiptNumber(),
                payment.getPaymentMethod().name(),
                payment.getPaidAt() != null ? payment.getPaidAt().toLocalDate().toString() : "Today");

        sendWhatsAppMessage(student.getPhone(), message, "PAYMENT_CONFIRMATION");
    }

    /**
     * Send attendance alert to student
     */
    public void sendAttendanceAlert(User student, String courseName, long absentCount) {
        String message = String.format(
                "🎓 *BrightNest Academy* - Attendance Alert ⚠️\n\n" +
                        "Hi %s,\n\n" +
                        "Your attendance in *%s* is low.\n" +
                        "Total absences: %d\n\n" +
                        "Regular attendance is important for your progress.\n" +
                        "Please reach out if you need any support.\n\n" +
                        "📞 +91-7204193980",
                student.getName(),
                courseName,
                absentCount);

        sendWhatsAppMessage(student.getPhone(), message, "ATTENDANCE_ALERT");
    }

    /**
     * Send a generic message
     */
    public void sendGenericMessage(String phone, String message) {
        sendWhatsAppMessage(phone, message, "GENERIC");
    }

    /**
     * Core WhatsApp message sender
     * 
     * When whatsapp.enabled=true, this will call the WhatsApp API.
     * Currently logs messages for development/testing.
     */
    private void sendWhatsAppMessage(String phone, String message, String type) {
        if (phone == null || phone.isBlank()) {
            log.warn("[WhatsApp-{}] No phone number available, skipping notification", type);
            return;
        }

        if (whatsappEnabled && !whatsappApiUrl.isBlank()) {
            try {
                // TODO: Implement actual WhatsApp API call
                // Example with Twilio:
                // Twilio.init(accountSid, authToken);
                // Message.creator(
                // new PhoneNumber("whatsapp:+91" + phone),
                // new PhoneNumber("whatsapp:" + senderNumber),
                // message
                // ).create();

                log.info("[WhatsApp-{}] SENT to {}: {}", type, maskPhone(phone), truncate(message));
            } catch (Exception e) {
                // Never let notification failure crash the main flow
                log.error("[WhatsApp-{}] FAILED to send to {}: {}", type, maskPhone(phone), e.getMessage());
            }
        } else {
            // Development mode - log masked phone for privacy
            log.info("[WhatsApp-{}] (DEV MODE) Would send to {}:\n{}", type, maskPhone(phone), message);
        }
    }

    /**
     * Check if WhatsApp integration is active
     */
    public boolean isWhatsAppEnabled() {
        return whatsappEnabled;
    }

    private String truncate(String text) {
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }

    /**
     * Mask phone number for log privacy (e.g., "9876543210" → "98****3210")
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6)
            return "****";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 6)
            return "****";
        return digits.substring(0, 2) + "****" + digits.substring(digits.length() - 4);
    }
}
