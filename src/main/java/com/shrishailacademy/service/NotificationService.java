package com.shrishailacademy.service;

import com.shrishailacademy.model.Enrollment;
import com.shrishailacademy.model.Payment;
import com.shrishailacademy.model.User;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Send enrollment confirmation via WhatsApp
     */
    public void sendEnrollmentConfirmation(Enrollment enrollment) {
        User student = enrollment.getUser();
        String message = String.format(
                """
                🎓 *BrightNest Academy*

                Hi %s! Welcome aboard! 🎉

                You have been successfully enrolled in:
                📚 *%s*
                ⏱ Duration: %s

                We look forward to an exciting learning journey with you!

                📞 Contact: +91-7204193980
                📧 info@brightnest-academy.com
                """,
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
                """
                🎓 *BrightNest Academy*

                Hi %s! Payment Confirmed! ✅

                💰 Amount: ₹%.0f
                📚 Course: %s
                🧾 Receipt: %s
                💳 Method: %s
                📅 Date: %s

                Thank you for your payment!

                📞 Contact: +91-7204193980
                """,
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
                """
                🎓 *BrightNest Academy* - Attendance Alert ⚠️

                Hi %s,

                Your attendance in *%s* is low.
                Total absences: %d

                Regular attendance is important for your progress.
                Please reach out if you need any support.

                📞 +91-7204193980
                """,
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
                sendViaApi(phone, message);
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

    private void sendViaApi(String phone, String message) throws Exception {
        if (whatsappApiKey.isBlank()) {
            throw new IllegalStateException("whatsapp.api.key is required when WhatsApp is enabled");
        }

        String payload = "{" +
                "\"to\":\"" + escapeJson(phone) + "\"," +
                "\"message\":\"" + escapeJson(message) + "\"" +
                (senderNumber == null || senderNumber.isBlank() ? "" : ",\"from\":\"" + escapeJson(senderNumber) + "\"") +
                "}";

        HttpRequest request = HttpRequest.newBuilder(URI.create(whatsappApiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + whatsappApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("WhatsApp API returned " + status + ": " + truncate(response.body()));
        }
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
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
