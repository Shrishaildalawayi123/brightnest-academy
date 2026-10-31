package com.shrishailacademy.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrishailacademy.model.*;

import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow;

class NotificationDeliveryTest {
    final NotificationService service = new NotificationService();
    final HttpClient client = mock(HttpClient.class);

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "httpClient", client);
        ReflectionTestUtils.setField(
                service, "whatsappApiUrl", "https://notification.example.invalid/messages");
        ReflectionTestUtils.setField(service, "whatsappApiKey", "test-only-token");
        ReflectionTestUtils.setField(service, "senderNumber", "test-sender");
    }

    @Test
    void disabledOrMissingPhoneNeverCallsExternalApi() {
        assertFalse(service.isWhatsAppEnabled());
        service.sendGenericMessage("9876543210", "Hello");
        service.sendGenericMessage(null, "Hello");
        service.sendGenericMessage(" ", "Hello");
        verifyNoInteractions(client);
    }

    @Test
    void enabledSenderProducesValidJsonForQuotesNewlinesAndTabs() throws Exception {
        ReflectionTestUtils.setField(service, "whatsappEnabled", true);
        var response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        doReturn(response).when(client).send(any(), any());
        String message = "A quoted \"lesson\"\nline\twith\\slash\r";
        service.sendGenericMessage("9876543210", message);
        var captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(captor.capture(), any());
        var request = captor.getValue();
        assertEquals("POST", request.method());
        assertEquals(
                "application/json", request.headers().firstValue("Content-Type").orElseThrow());
        StringBuilder json = new StringBuilder();
        request.bodyPublisher()
                .orElseThrow()
                .subscribe(
                        new Flow.Subscriber<ByteBuffer>() {
                            public void onSubscribe(Flow.Subscription s) {
                                s.request(Long.MAX_VALUE);
                            }

                            public void onNext(ByteBuffer b) {
                                json.append(StandardCharsets.UTF_8.decode(b));
                            }

                            public void onError(Throwable t) {
                                fail(t);
                            }

                            public void onComplete() {}
                        });
        var body = new ObjectMapper().readTree(json.toString());
        assertEquals(message, body.get("message").asText());
        assertEquals("test-sender", body.get("from").asText());
        assertEquals("9876543210", body.get("to").asText());
    }

    @Test
    void failedProviderAndMissingCredentialsDoNotCrashEnrollmentFlow() throws Exception {
        ReflectionTestUtils.setField(service, "whatsappEnabled", true);
        var response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(503);
        when(response.body()).thenReturn("Temporarily unavailable");
        doReturn(response).when(client).send(any(), any());
        assertDoesNotThrow(() -> service.sendGenericMessage("9876543210", "x".repeat(120)));
        ReflectionTestUtils.setField(service, "whatsappApiKey", "");
        assertDoesNotThrow(() -> service.sendGenericMessage("123", "Retry"));
        verify(client, times(1)).send(any(), any());
    }

    @Test
    void enrollmentPaymentAndAttendanceNotificationsUseTheirBusinessFields() {
        User user = new User();
        user.setName("Student");
        user.setPhone("9876543210");
        Course course = new Course();
        course.setTitle("Maths");
        course.setDuration("12 months");
        Enrollment enrollment = new Enrollment();
        enrollment.setUser(user);
        enrollment.setCourse(course);
        Payment payment = new Payment();
        payment.setUser(user);
        payment.setCourse(course);
        payment.setAmount(new BigDecimal("1000"));
        payment.setPaymentMethod(Payment.PaymentMethod.UPI);
        payment.setReceiptNumber("receipt-test");
        assertDoesNotThrow(() -> service.sendEnrollmentConfirmation(enrollment));
        assertDoesNotThrow(() -> service.sendPaymentConfirmation(payment));
        assertDoesNotThrow(() -> service.sendAttendanceAlert(user, "Maths", 3));
        verifyNoInteractions(client);
    }
}
