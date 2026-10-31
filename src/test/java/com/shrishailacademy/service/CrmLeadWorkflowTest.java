package com.shrishailacademy.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.shrishailacademy.dto.*;
import com.shrishailacademy.exception.BusinessException;
import com.shrishailacademy.model.*;
import com.shrishailacademy.repository.*;
import com.shrishailacademy.tenant.TenantContext;

import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

import java.time.*;
import java.util.*;

class CrmLeadWorkflowTest {
    final ContactMessageRepository contacts = mock(ContactMessageRepository.class);
    final DemoBookingRepository demos = mock(DemoBookingRepository.class);
    final CounselingRequestRepository counseling = mock(CounselingRequestRepository.class);
    final CrmLeadPipelineRepository pipelines = mock(CrmLeadPipelineRepository.class);
    final ChatbotQualifiedLeadRepository chat = mock(ChatbotQualifiedLeadRepository.class);
    final ContactService contactService = mock(ContactService.class);
    final DemoBookingService demoService = mock(DemoBookingService.class);
    final CounselingService counselingService = mock(CounselingService.class);
    final TenantService tenants = mock(TenantService.class);
    final CrmLeadService service =
            new CrmLeadService(
                    contacts,
                    demos,
                    counseling,
                    pipelines,
                    chat,
                    contactService,
                    demoService,
                    counselingService,
                    tenants);
    ContactMessage contact;
    CrmLeadPipeline pipeline;

    @BeforeEach
    void setup() {
        TenantContext.set(7L, "a");
        Tenant tenant = new Tenant();
        tenant.setId(7L);
        when(tenants.requireCurrentTenant()).thenReturn(tenant);
        contact = new ContactMessage();
        contact.setId(1L);
        contact.setName("Parent, One");
        contact.setEmail("parent@example.com");
        contact.setSubject("Maths");
        contact.setMessage("Help with algebra");
        contact.setStatus(ContactMessage.Status.NEW);
        contact.setCreatedAt(LocalDate.now().atTime(23, 59));
        DemoBooking demo = new DemoBooking();
        demo.setId(2L);
        demo.setStudentName("Student Two");
        demo.setParentName("Parent Two");
        demo.setRequirements("ICSE");
        demo.setMessage("Evenings");
        demo.setStatus(DemoBooking.Status.SCHEDULED);
        demo.setCreatedAt(LocalDateTime.now().minusDays(1));
        CounselingRequest request = new CounselingRequest();
        request.setId(3L);
        request.setStudentName("Student Three");
        request.setStatus(CounselingRequest.Status.COMPLETED);
        request.setCreatedAt(LocalDateTime.now().minusDays(2));
        ChatbotQualifiedLead lead = new ChatbotQualifiedLead();
        lead.setId(4L);
        lead.setStatus(ChatbotQualifiedLead.Status.NEW);
        lead.setCreatedAt(LocalDateTime.now().minusDays(3));
        pipeline = new CrmLeadPipeline();
        pipeline.setSource("contact");
        pipeline.setSourceId(1L);
        pipeline.setAssignee("Advisor One");
        pipeline.setFollowUpStatus(CrmLeadPipeline.FollowUpStatus.PENDING);
        pipeline.setFollowUpAt(LocalDate.now().atTime(12, 0));
        when(contacts.findByTenantIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(contact));
        when(demos.findByTenantIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(demo));
        when(counseling.findByTenantIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(request));
        when(chat.findByTenantIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(lead));
        when(pipelines.findByTenantId(7L)).thenReturn(List.of(pipeline));
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    List<CrmLeadDto> find(
            String source,
            String status,
            String query,
            LocalDate from,
            LocalDate to,
            String assignee,
            String follow) {
        return service.getLeads(source, status, query, from, to, assignee, follow);
    }

    @Test
    void combinesSourcesAndAppliesInclusiveDateStatusSearchAndAssigneeFilters() {
        assertEquals(4, find("all", "all", null, null, null, null, null).size());
        var selected =
                find(
                        " CONTACT ",
                        " new ",
                        "ALGEBRA",
                        LocalDate.now(),
                        LocalDate.now(),
                        "advisor",
                        "pending");
        assertEquals(1, selected.size());
        assertEquals("parent@example.com", selected.get(0).email());
        assertTrue(find("contact", null, "no-match", null, null, null, null).isEmpty());
        assertTrue(
                find("contact", null, null, LocalDate.now().plusDays(1), null, null, null)
                        .isEmpty());
        assertTrue(
                find("contact", null, null, null, LocalDate.now().minusDays(1), null, null)
                        .isEmpty());
        assertTrue(find("contact", null, null, null, null, "other", null).isEmpty());
        assertTrue(find("contact", null, null, null, null, null, "completed").isEmpty());
        contact.setCreatedAt(null);
        assertTrue(find("contact", null, null, null, null, null, null).isEmpty());
    }

    @Test
    void reminderStatsDistinguishTodayOverdueUpcomingAndCompleted() {
        var stats = service.getStats();
        assertEquals(4L, stats.get("totalLeads"));
        assertEquals(2L, stats.get("newLeads"));
        assertEquals(2L, stats.get("contactedLeads"));
        assertEquals(3L, stats.get("highIntentLeads"));
        assertEquals(1L, stats.get("reminderToday"));
        pipeline.setFollowUpAt(LocalDateTime.now().minusDays(1));
        assertEquals(1L, service.getStats().get("reminderOverdue"));
        pipeline.setFollowUpAt(LocalDateTime.now().plusDays(7));
        assertEquals(1L, service.getStats().get("reminderNext7Days"));
        pipeline.setFollowUpStatus(CrmLeadPipeline.FollowUpStatus.COMPLETED);
        assertEquals(0L, service.getStats().get("reminderNext7Days"));
    }

    @Test
    void sourceStatusUpdatesRouteToTheCorrectTenantAwareService() {
        service.updateLeadStatus("CONTACT", 1L, "READ");
        verify(contactService).updateMessageStatus(1L, "READ");
        service.updateLeadStatus("demo", 2L, "SCHEDULED");
        verify(demoService).updateStatus(2L, "SCHEDULED");
        service.updateLeadStatus("counseling", 3L, "CONTACTED");
        verify(counselingService).updateStatus(3L, "CONTACTED");
        ChatbotQualifiedLead lead = new ChatbotQualifiedLead();
        when(chat.findByIdAndTenantId(4L, 7L)).thenReturn(Optional.of(lead));
        service.updateLeadStatus("chatbot", 4L, "enrolled");
        assertEquals(ChatbotQualifiedLead.Status.ENROLLED, lead.getStatus());
        verify(chat).save(lead);
        assertThrows(BusinessException.class, () -> service.updateLeadStatus("unknown", 1L, "NEW"));
        assertThrows(BusinessException.class, () -> service.updateLeadStatus(" ", 1L, "NEW"));
        assertThrows(BusinessException.class, () -> service.updateLeadStatus("contact", 1L, " "));
        assertThrows(
                BusinessException.class, () -> service.updateLeadStatus("chatbot", 4L, "invalid"));
        assertThrows(
                BusinessException.class, () -> service.updateLeadStatus("chatbot", 99L, "NEW"));
    }

    @Test
    void pipelineCreationUpdateAndValidationPreserveTenantAndFollowup() {
        var request =
                new CrmLeadPipelineUpdateRequest(
                        "<b>Advisor</b>",
                        LocalDateTime.now(),
                        "pending",
                        "<script>bad()</script>Call parent");
        service.updateLeadPipeline("contact", 1L, request);
        var captor = ArgumentCaptor.forClass(CrmLeadPipeline.class);
        verify(pipelines).save(captor.capture());
        assertEquals(7L, captor.getValue().getTenant().getId());
        assertTrue(captor.getValue().getAssignee().contains("Advisor"));
        assertFalse(captor.getValue().getAssignee().contains("<b>"));
        assertEquals(CrmLeadPipeline.FollowUpStatus.PENDING, captor.getValue().getFollowUpStatus());
        assertFalse(captor.getValue().getFollowUpNotes().contains("<script>"));
        when(pipelines.findByTenantIdAndSourceAndSourceId(7L, "contact", 1L))
                .thenReturn(Optional.of(pipeline));
        service.updateLeadPipeline(
                "contact", 1L, new CrmLeadPipelineUpdateRequest(null, null, null, null));
        assertEquals(CrmLeadPipeline.FollowUpStatus.NONE, pipeline.getFollowUpStatus());
        assertThrows(
                BusinessException.class,
                () ->
                        service.updateLeadPipeline(
                                "contact",
                                1L,
                                new CrmLeadPipelineUpdateRequest(null, null, "invalid", null)));
    }

    @Test
    void exportIsAValidCsvWithQuotedMultilineContent() {
        contact.setMessage("Parent says \"hello\"\nPlease call");
        var csv = service.exportCsv("contact", null, null, null, null, null, null);
        assertTrue(csv.startsWith("Source,Lead ID,Name"));
        assertTrue(csv.contains("\"Parent, One\""));
        assertTrue(csv.contains("Parent says \"\"hello\"\"\nPlease call"));
        assertTrue(csv.contains("\"Advisor One\""));
    }
}
