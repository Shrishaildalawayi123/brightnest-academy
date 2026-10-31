package com.shrishailacademy.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.shrishailacademy.dto.*;
import com.shrishailacademy.model.*;
import com.shrishailacademy.repository.*;
import com.shrishailacademy.tenant.TenantContext;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.util.*;

class ChatbotJourneyTest {
    final ChatbotMessageRepository messages = mock(ChatbotMessageRepository.class);
    final ChatbotQualifiedLeadRepository leads = mock(ChatbotQualifiedLeadRepository.class);
    final TenantService tenants = mock(TenantService.class);
    final ChatbotService service = new ChatbotService(messages, leads, tenants);

    @BeforeEach
    void setup() {
        TenantContext.set(7L, "a");
        Tenant t = new Tenant();
        t.setId(7L);
        when(tenants.requireCurrentTenant()).thenReturn(t);
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    Map<String, Object> ask(String message, String session) {
        return service.respond(new ChatbotRequest(message, session));
    }

    @Test
    void feeQuestionPromptsForGradeThenCapturesOneQualifiedLead() {
        assertEquals(
                false, ask("What are your monthly fees?", "parent").get("qualifiedLeadCaptured"));
        assertTrue(ask("online", "parent").get("reply").toString().contains("grade first"));
        var answer = ask("Grade 6 ICSE maths", "parent");
        assertEquals(true, answer.get("qualifiedLeadCaptured"));
        assertTrue(answer.get("reply").toString().contains("Progress Track"));
        var captured = ArgumentCaptor.forClass(ChatbotQualifiedLead.class);
        verify(leads).save(captured.capture());
        assertEquals("ICSE", captured.getValue().getBoard());
        assertEquals("Mathematics", captured.getValue().getSubjectInterest());
        assertEquals(7L, captured.getValue().getTenant().getId());
        ask("hello", "parent");
        verify(leads, times(1)).save(any());
    }

    @ParameterizedTest
    @CsvSource({"2,Foundation Program", "7,Progress Track", "10,Exam Accelerator"})
    void feeRecommendationMatchesSchoolStage(int grade, String plan) {
        var reply = ask("fees for grade " + grade + " CBSE science", "flow");
        assertTrue(reply.get("reply").toString().contains(plan));
        assertTrue(reply.get("reply").toString().contains("Science"));
        assertEquals(true, reply.get("qualifiedLeadCaptured"));
    }

    @Test
    void feeConversationStateDoesNotLeakAcrossTenants() {
        ask("fees", "shared-session");
        TenantContext.set(8L, "b");
        assertEquals(false, ask("Grade 6", "shared-session").get("qualifiedLeadCaptured"));
        verify(leads, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({
        "where are you located,Kumaraswamy Layout",
        "online mode,Google Meet",
        "demo booking,demo class",
        "which subjects,Mathematics",
        "what courses,Mathematics",
        "human advisor,WhatsApp"
    })
    void faqAndFallbackAnswersAreLoggedWithoutCreatingQualifiedLeads(
            String message, String expected) {
        var answer = ask(message, null);
        assertNotNull(UUID.fromString(answer.get("sessionId").toString()));
        assertTrue(answer.get("reply").toString().contains(expected));
        assertEquals(false, answer.get("qualifiedLeadCaptured"));
        verify(messages).save(any(ChatbotMessage.class));
        verify(leads, never()).save(any());
    }

    @Test
    void unknownQuestionHasLowConfidenceAndSafeSuggestions() {
        var result = ask("xyzzy", "unknown");
        assertEquals("low", result.get("confidence"));
        assertFalse(((List<?>) result.get("suggestions")).isEmpty());
    }

    @Test
    void enrichmentRejectsMissingContactOrUnknownTenantSession() {
        assertEquals(
                false,
                service.enrichLeadContact(
                                new ChatbotLeadEnrichmentRequest("s", null, null, null, null))
                        .get("updated"));
        assertEquals(
                false,
                service.enrichLeadContact(
                                new ChatbotLeadEnrichmentRequest("s", "Parent", null, null, null))
                        .get("updated"));
        verify(leads).findFirstByTenantIdAndSessionIdOrderByCreatedAtDesc(7L, "s");
        verify(leads, never()).save(any());
    }

    @Test
    void enrichmentUpdatesOnlyResolvedTenantLeadAndNormalizesContact() {
        var lead = new ChatbotQualifiedLead();
        lead.setId(10L);
        when(leads.findFirstByTenantIdAndSessionIdOrderByCreatedAtDesc(7L, "s"))
                .thenReturn(Optional.of(lead));
        var result =
                service.enrichLeadContact(
                        new ChatbotLeadEnrichmentRequest(
                                "s",
                                "<b>Parent</b>",
                                "PARENT@example.com",
                                "+91 98765-43210",
                                "Grade 8"));
        assertEquals(true, result.get("updated"));
        assertEquals(10L, result.get("leadId"));
        assertEquals("+919876543210", lead.getPhone());
        assertEquals("Grade 8", lead.getGrade());
        assertFalse(lead.getLeadName().contains("<b>"));
        verify(leads).save(lead);
    }
}
