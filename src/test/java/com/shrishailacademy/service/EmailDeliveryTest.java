package com.shrishailacademy.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.shrishailacademy.model.*;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Properties;

class EmailDeliveryTest {
    final JavaMailSender sender = mock(JavaMailSender.class);
    final EmailService service = new EmailService(sender);

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "fromEmail", "academy@example.com");
        ReflectionTestUtils.setField(service, "adminEmail", "admin@example.com");
        when(sender.createMimeMessage())
                .thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void contactAcknowledgementAndDemoConfirmationReachCorrectRecipients() throws Exception {
        ContactMessage contact = new ContactMessage();
        contact.setName("Parent");
        contact.setEmail("parent@example.com");
        contact.setSubject("Maths");
        service.sendContactAcknowledgement(contact);
        DemoBooking demo = new DemoBooking();
        demo.setStudentName("Student");
        demo.setEmail("parent@example.com");
        demo.setSubject("English");
        demo.setDemoFee(100);
        demo.setClassMode(DemoBooking.ClassMode.ONLINE);
        service.sendDemoBookingConfirmation(demo);
        var messages = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender, times(2)).send(messages.capture());
        var acknowledgement = messages.getAllValues().get(0);
        var confirmation = messages.getAllValues().get(1);
        assertEquals(
                "parent@example.com",
                acknowledgement.getRecipients(Message.RecipientType.TO)[0].toString());
        assertTrue(acknowledgement.getContent().toString().contains("Maths"));
        assertEquals(
                "admin@example.com",
                confirmation.getRecipients(Message.RecipientType.BCC)[0].toString());
        assertTrue(confirmation.getContent().toString().contains("INR 100"));
    }

    @Test
    void receiptIncludesConfirmedAmountCourseAndPaymentReference() throws Exception {
        User user = new User();
        user.setName("Student");
        user.setEmail("student@example.com");
        Course course = new Course();
        course.setTitle("Science");
        Payment p = new Payment();
        p.setCourse(course);
        p.setAmount(new BigDecimal("1200.50"));
        p.setReceiptNumber("BN-123");
        p.setPaidAt(LocalDateTime.of(2026, 10, 1, 10, 0));
        p.setPaymentMethod(Payment.PaymentMethod.UPI);
        service.sendPaymentReceipt(p, user);
        var captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        assertTrue(captor.getValue().getSubject().contains("BN-123"));
        String body = captor.getValue().getContent().toString();
        assertTrue(body.contains("1200.50"));
        assertTrue(body.contains("Science"));
        assertTrue(body.contains("01 Oct 2026"));
    }

    @Test
    void missingConfigurationOrRecipientsSkipsOutboundMail() throws Exception {
        ReflectionTestUtils.setField(service, "fromEmail", "");
        service.sendAdminNewLeadAlert("Contact", "Parent", "test");
        service.sendContactAcknowledgement(new ContactMessage());
        service.sendDemoBookingConfirmation(new DemoBooking());
        service.sendPaymentReceipt(new Payment(), null);
        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test
    void adminAlertIsAddressedToAdminAndDeliveryFailurePropagatesToCaller() throws Exception {
        service.sendAdminNewLeadAlert("Contact", "Parent", "parent@example.com");
        var captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        assertEquals(
                "admin@example.com",
                captor.getValue().getRecipients(Message.RecipientType.TO)[0].toString());
        doThrow(new MailSendException("provider down")).when(sender).send(any(MimeMessage.class));
        assertThrows(
                MailSendException.class,
                () -> service.sendAdminNewLeadAlert("Demo", "Parent", "test"));
    }
}
