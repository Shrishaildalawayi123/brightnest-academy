package com.shrishailacademy.service;

import com.shrishailacademy.model.ContactMessage;
import com.shrishailacademy.model.DemoBooking;
import com.shrishailacademy.model.Payment;
import com.shrishailacademy.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@ConditionalOnProperty(name = "spring.mail.username", matchIfMissing = false)
public class EmailService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${email.admin:}")
    private String adminEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendDemoBookingConfirmation(DemoBooking booking) throws MessagingException {
        if (!isConfigured() || !StringUtils.hasText(booking.getEmail())) {
            return;
        }

        String subject = "Demo booking received - BrightNest Academy";
        String body = """
                <p>Hi %s,</p>
                <p>We have received your demo booking for <strong>%s</strong>.</p>
                <p>Our team will contact you within 24 hours to confirm the schedule. Demo fee: INR %d.</p>
                <p>Preferred mode: %s</p>
                <p>Thank you,<br>BrightNest Academy</p>
                """.formatted(
                safe(booking.getStudentName()),
                safe(booking.getSubject()),
                booking.getDemoFee(),
                booking.getClassMode().name());

        sendHtmlMessage(booking.getEmail(), subject, body, true);
    }

    public void sendContactAcknowledgement(ContactMessage message) throws MessagingException {
        if (!isConfigured() || !StringUtils.hasText(message.getEmail())) {
            return;
        }

        String subject = "We received your message - BrightNest Academy";
        String body = """
                <p>Hi %s,</p>
                <p>Thank you for contacting BrightNest Academy.</p>
                <p>We have received your message about <strong>%s</strong> and will get back to you soon.</p>
                <p>Regards,<br>BrightNest Academy</p>
                """.formatted(safe(message.getName()), safe(message.getSubject()));

        sendHtmlMessage(message.getEmail(), subject, body, false);
    }

    public void sendPaymentReceipt(Payment payment, User user) throws MessagingException {
        if (!isConfigured() || user == null || !StringUtils.hasText(user.getEmail())) {
            return;
        }

        String courseName = payment.getCourse() != null ? safe(payment.getCourse().getTitle()) : "your course";
        BigDecimal amount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
        String paidAt = payment.getPaidAt() != null ? payment.getPaidAt().format(DATE_TIME_FORMATTER) : "Pending";
        String subject = "Payment receipt - " + payment.getReceiptNumber();
        String body = """
                <html>
                <body style="font-family:Arial,sans-serif;color:#1f2937;line-height:1.6;">
                  <h2 style="margin-bottom:8px;">Payment Confirmed</h2>
                  <p>Hi %s,</p>
                  <p>Your payment for <strong>%s</strong> has been confirmed.</p>
                  <table style="border-collapse:collapse;margin:16px 0;">
                    <tr><td style="padding:6px 12px 6px 0;"><strong>Receipt</strong></td><td>%s</td></tr>
                    <tr><td style="padding:6px 12px 6px 0;"><strong>Amount</strong></td><td>INR %s</td></tr>
                    <tr><td style="padding:6px 12px 6px 0;"><strong>Method</strong></td><td>%s</td></tr>
                    <tr><td style="padding:6px 12px 6px 0;"><strong>Paid At</strong></td><td>%s</td></tr>
                  </table>
                  <p>Thank you for learning with BrightNest Academy.</p>
                </body>
                </html>
                """.formatted(
                safe(user.getName()),
                courseName,
                safe(payment.getReceiptNumber()),
                amount.toPlainString(),
                payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "ONLINE",
                paidAt);

        sendHtmlMessage(user.getEmail(), subject, body, false);
    }

    public void sendAdminNewLeadAlert(String leadType, String name, String contact) throws MessagingException {
        if (!isConfigured() || !StringUtils.hasText(adminEmail)) {
            return;
        }

        String subject = "New " + safe(leadType) + " lead received";
        String body = """
                <p>A new lead was captured.</p>
                <p><strong>Type:</strong> %s</p>
                <p><strong>Name:</strong> %s</p>
                <p><strong>Contact:</strong> %s</p>
                """.formatted(safe(leadType), safe(name), safe(contact));

        sendHtmlMessage(adminEmail, subject, body, false);
    }

    private void sendHtmlMessage(String to, String subject, String body, boolean bccAdmin) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true);
        helper.setFrom(fromEmail);
        if (bccAdmin && shouldBccAdmin(to)) {
            helper.setBcc(adminEmail);
        }
        mailSender.send(mimeMessage);
    }

    private boolean isConfigured() {
        return StringUtils.hasText(fromEmail);
    }

    private boolean shouldBccAdmin(String to) {
        return StringUtils.hasText(adminEmail) && !adminEmail.equalsIgnoreCase(to);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
