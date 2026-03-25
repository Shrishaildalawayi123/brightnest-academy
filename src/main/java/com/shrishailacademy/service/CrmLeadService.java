package com.shrishailacademy.service;

import com.shrishailacademy.dto.CrmLeadDto;
import com.shrishailacademy.dto.CrmLeadPipelineUpdateRequest;
import com.shrishailacademy.model.ChatbotQualifiedLead;
import com.shrishailacademy.exception.BusinessException;
import com.shrishailacademy.model.ContactMessage;
import com.shrishailacademy.model.CrmLeadPipeline;
import com.shrishailacademy.model.CounselingRequest;
import com.shrishailacademy.model.DemoBooking;
import com.shrishailacademy.repository.ChatbotQualifiedLeadRepository;
import com.shrishailacademy.repository.ContactMessageRepository;
import com.shrishailacademy.repository.CrmLeadPipelineRepository;
import com.shrishailacademy.repository.CounselingRequestRepository;
import com.shrishailacademy.repository.DemoBookingRepository;
import com.shrishailacademy.tenant.TenantContext;
import com.shrishailacademy.util.InputSanitizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class CrmLeadService {

    private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ContactMessageRepository contactMessageRepository;
    private final DemoBookingRepository demoBookingRepository;
    private final CounselingRequestRepository counselingRequestRepository;
    private final CrmLeadPipelineRepository crmLeadPipelineRepository;
    private final ChatbotQualifiedLeadRepository chatbotQualifiedLeadRepository;
    private final ContactService contactService;
    private final DemoBookingService demoBookingService;
    private final CounselingService counselingService;
    private final TenantService tenantService;

    public CrmLeadService(
            ContactMessageRepository contactMessageRepository,
            DemoBookingRepository demoBookingRepository,
            CounselingRequestRepository counselingRequestRepository,
            CrmLeadPipelineRepository crmLeadPipelineRepository,
            ChatbotQualifiedLeadRepository chatbotQualifiedLeadRepository,
            ContactService contactService,
            DemoBookingService demoBookingService,
            CounselingService counselingService,
            TenantService tenantService) {
        this.contactMessageRepository = contactMessageRepository;
        this.demoBookingRepository = demoBookingRepository;
        this.counselingRequestRepository = counselingRequestRepository;
        this.crmLeadPipelineRepository = crmLeadPipelineRepository;
        this.chatbotQualifiedLeadRepository = chatbotQualifiedLeadRepository;
        this.contactService = contactService;
        this.demoBookingService = demoBookingService;
        this.counselingService = counselingService;
        this.tenantService = tenantService;
    }

    public List<CrmLeadDto> getLeads(
            String source,
            String status,
            String query,
            LocalDate fromDate,
            LocalDate toDate,
            String assignee,
            String followUpStatus) {
        Long tenantId = TenantContext.requireTenantId();
        String normalizedSource = normalizeNullable(source);
        String normalizedStatus = normalizeNullable(status);
        String normalizedQuery = normalizeNullable(query);
        String normalizedAssignee = normalizeNullable(assignee);
        String normalizedFollowUpStatus = normalizeNullable(followUpStatus);
        LocalDateTime fromDateTime = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate == null ? null : toDate.plusDays(1).atStartOfDay().minusNanos(1);

        return leadStreamForTenant(tenantId)
                .filter(lead -> matchesSource(lead, normalizedSource))
                .filter(lead -> matchesStatus(lead, normalizedStatus))
                .filter(lead -> matchesQuery(lead, normalizedQuery))
                .filter(lead -> matchesDateRange(lead, fromDateTime, toDateTime))
                .filter(lead -> matchesAssignee(lead, normalizedAssignee))
                .filter(lead -> matchesFollowUpStatus(lead, normalizedFollowUpStatus))
                .sorted(Comparator.comparing(CrmLeadDto::createdAt, Comparator.nullsLast(LocalDateTime::compareTo))
                        .reversed())
                .toList();
    }

    public Map<String, Object> getStats() {
        Long tenantId = TenantContext.requireTenantId();
        List<CrmLeadDto> leads = leadStreamForTenant(tenantId)
                .sorted(Comparator.comparing(CrmLeadDto::createdAt, Comparator.nullsLast(LocalDateTime::compareTo))
                        .reversed())
                .toList();

        LocalDate today = LocalDate.now();
        long reminderToday = leads.stream().filter(lead -> isReminderToday(lead, today)).count();
        long reminderOverdue = leads.stream().filter(lead -> isReminderOverdue(lead, today)).count();
        long reminderNext7Days = leads.stream().filter(lead -> isReminderInNext7Days(lead, today)).count();

        long totalLeads = leads.size();
        long newLeads = leads.stream().filter(this::isNewLead).count();
        long contactedLeads = leads.stream().filter(this::isContactedLead).count();
        long highIntentLeads = leads.stream().filter(this::isHighIntentLead).count();

        Map<String, Long> leadsBySource = leads.stream()
                .collect(Collectors.groupingBy(CrmLeadDto::source, LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> leadsByStatus = leads.stream()
                .collect(Collectors.groupingBy(lead -> safeValue(lead.status()), LinkedHashMap::new, Collectors.counting()));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalLeads", totalLeads);
        stats.put("newLeads", newLeads);
        stats.put("contactedLeads", contactedLeads);
        stats.put("highIntentLeads", highIntentLeads);
        stats.put("reminderToday", reminderToday);
        stats.put("reminderOverdue", reminderOverdue);
        stats.put("reminderNext7Days", reminderNext7Days);
        stats.put("bySource", leadsBySource);
        stats.put("byStatus", leadsByStatus);
        return stats;
    }

    @Transactional
    public void updateLeadStatus(String source, Long sourceId, String status) {
        String normalizedSource = requireSupportedSource(source);
        String normalizedStatus = requireNonBlank(status, "Status is required");

        switch (normalizedSource) {
            case "contact" -> contactService.updateMessageStatus(sourceId, normalizedStatus);
            case "demo" -> demoBookingService.updateStatus(sourceId, normalizedStatus);
            case "counseling" -> counselingService.updateStatus(sourceId, normalizedStatus);
            case "chatbot" -> updateChatbotLeadStatus(sourceId, normalizedStatus);
            default -> throw new BusinessException("Unsupported lead source", "UNSUPPORTED_SOURCE");
        }
    }

    @Transactional
    public void updateLeadPipeline(String source, Long sourceId, CrmLeadPipelineUpdateRequest request) {
        String normalizedSource = requireSupportedSource(source);
        Long tenantId = TenantContext.requireTenantId();

        CrmLeadPipeline pipeline = crmLeadPipelineRepository
                .findByTenantIdAndSourceAndSourceId(tenantId, normalizedSource, sourceId)
                .orElseGet(() -> {
                    CrmLeadPipeline created = new CrmLeadPipeline();
                    created.setTenant(tenantService.requireCurrentTenant());
                    created.setSource(normalizedSource);
                    created.setSourceId(sourceId);
                    return created;
                });

        pipeline.setAssignee(InputSanitizer.sanitizeAndTruncateNullable(request.assignee(), 120));
        pipeline.setFollowUpAt(request.followUpAt());
        pipeline.setFollowUpNotes(InputSanitizer.sanitizeAndTruncateNullable(request.followUpNotes(), 1000));
        pipeline.setFollowUpStatus(parseFollowUpStatus(request.followUpStatus()));
        crmLeadPipelineRepository.save(pipeline);
    }

    public String exportCsv(
            String source,
            String status,
            String query,
            LocalDate fromDate,
            LocalDate toDate,
            String assignee,
            String followUpStatus) {
        List<CrmLeadDto> leads = getLeads(source, status, query, fromDate, toDate, assignee, followUpStatus);
        StringBuilder builder = new StringBuilder();
        builder.append("Source,Lead ID,Name,Guardian,Email,Phone,Subject,Grade,Board,Status,Assignee,Follow-up Status,Follow-up At,Created At,Summary\n");
        for (CrmLeadDto lead : leads) {
            builder.append(csv(lead.sourceLabel())).append(',')
                    .append(csv(String.valueOf(lead.sourceId()))).append(',')
                    .append(csv(lead.leadName())).append(',')
                    .append(csv(lead.guardianName())).append(',')
                    .append(csv(lead.email())).append(',')
                    .append(csv(lead.phone())).append(',')
                    .append(csv(lead.subject())).append(',')
                    .append(csv(lead.grade())).append(',')
                    .append(csv(lead.board())).append(',')
                    .append(csv(lead.status())).append(',')
                        .append(csv(lead.assignee())).append(',')
                        .append(csv(lead.followUpStatus())).append(',')
                        .append(csv(formatDate(lead.followUpAt()))).append(',')
                    .append(csv(formatDate(lead.createdAt()))).append(',')
                    .append(csv(lead.summary()))
                    .append('\n');
        }
        return builder.toString();
    }

    private Stream<CrmLeadDto> leadStreamForTenant(Long tenantId) {
                Map<String, CrmLeadPipeline> pipelineByKey = new HashMap<>();
                crmLeadPipelineRepository.findByTenantId(tenantId)
                    .forEach(pipeline -> pipelineByKey.put(key(pipeline.getSource(), pipeline.getSourceId()), pipeline));

        List<CrmLeadDto> leads = new ArrayList<>();
        contactMessageRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                    .map(contact -> mapContactLead(contact, pipelineByKey.get(key("contact", contact.getId()))))
                .forEach(leads::add);
        demoBookingRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                    .map(demo -> mapDemoLead(demo, pipelineByKey.get(key("demo", demo.getId()))))
                .forEach(leads::add);
        counselingRequestRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                    .map(request -> mapCounselingLead(request, pipelineByKey.get(key("counseling", request.getId()))))
                    .forEach(leads::add);
                chatbotQualifiedLeadRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                    .stream()
                    .map(lead -> mapChatbotLead(lead, pipelineByKey.get(key("chatbot", lead.getId()))))
                .forEach(leads::add);
        return leads.stream();
    }

                private CrmLeadDto mapContactLead(ContactMessage message, CrmLeadPipeline pipeline) {
        return new CrmLeadDto(
                "contact",
                message.getId(),
                "Contact Form",
                message.getName(),
                null,
                message.getEmail(),
                message.getPhone(),
                message.getSubject(),
                null,
                null,
                message.getStatus().name(),
                assignee(pipeline),
                followUpStatus(pipeline),
                followUpAt(pipeline),
                followUpNotes(pipeline),
                message.getMessage(),
                message.getCreatedAt());
    }

    private CrmLeadDto mapDemoLead(DemoBooking booking, CrmLeadPipeline pipeline) {
        String summary = Stream.of(booking.getRequirements(), booking.getMessage())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" | "));
        return new CrmLeadDto(
                "demo",
                booking.getId(),
                "Demo Booking",
                booking.getStudentName(),
                booking.getParentName(),
                booking.getEmail(),
                booking.getPhone(),
                booking.getSubject(),
                booking.getGrade(),
                booking.getBoard(),
                booking.getStatus().name(),
                assignee(pipeline),
                followUpStatus(pipeline),
                followUpAt(pipeline),
                followUpNotes(pipeline),
                summary,
                booking.getCreatedAt());
    }

    private CrmLeadDto mapCounselingLead(CounselingRequest request, CrmLeadPipeline pipeline) {
        return new CrmLeadDto(
                "counseling",
                request.getId(),
                "Counseling Request",
                request.getStudentName(),
                null,
                null,
                request.getParentPhone(),
                "Academic Counseling",
                request.getStudentClass(),
                request.getBoard(),
                request.getStatus().name(),
                assignee(pipeline),
                followUpStatus(pipeline),
                followUpAt(pipeline),
                followUpNotes(pipeline),
                "Requested counseling callback",
                request.getCreatedAt());
    }

    private CrmLeadDto mapChatbotLead(ChatbotQualifiedLead lead, CrmLeadPipeline pipeline) {
        return new CrmLeadDto(
                "chatbot",
                lead.getId(),
                "AI Admission Assistant",
                StringUtils.hasText(lead.getLeadName()) ? lead.getLeadName() : "Anonymous Parent",
                null,
                lead.getEmail(),
                lead.getPhone(),
                StringUtils.hasText(lead.getSubjectInterest()) ? lead.getSubjectInterest() : "General Inquiry",
                lead.getGrade(),
                lead.getBoard(),
                lead.getStatus().name(),
                assignee(pipeline),
                followUpStatus(pipeline),
                followUpAt(pipeline),
                followUpNotes(pipeline),
                lead.getRecommendedPlan(),
                lead.getCreatedAt());
    }

    private boolean matchesSource(CrmLeadDto lead, String source) {
        return !StringUtils.hasText(source) || "all".equals(source) || lead.source().equals(source);
    }

    private boolean matchesStatus(CrmLeadDto lead, String status) {
        return !StringUtils.hasText(status) || "all".equals(status) || safeValue(lead.status()).equals(status);
    }

    private boolean matchesQuery(CrmLeadDto lead, String query) {
        if (!StringUtils.hasText(query)) {
            return true;
        }
        return Stream.of(
                lead.leadName(),
                lead.guardianName(),
                lead.email(),
                lead.phone(),
                lead.subject(),
                lead.grade(),
                lead.board(),
                lead.assignee(),
                lead.followUpStatus(),
                lead.summary(),
                lead.status(),
                lead.sourceLabel())
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains(query));
    }

    private boolean isNewLead(CrmLeadDto lead) {
        return switch (lead.source()) {
            case "contact" -> "NEW".equals(lead.status());
            case "demo" -> "PENDING".equals(lead.status());
            case "counseling" -> "NEW".equals(lead.status());
            case "chatbot" -> "NEW".equals(lead.status());
            default -> false;
        };
    }

    private boolean isContactedLead(CrmLeadDto lead) {
        return switch (lead.source()) {
            case "contact" -> "READ".equals(lead.status()) || "REPLIED".equals(lead.status());
            case "demo" -> "SCHEDULED".equals(lead.status()) || "COMPLETED".equals(lead.status());
            case "counseling" -> "CONTACTED".equals(lead.status()) || "COMPLETED".equals(lead.status());
            case "chatbot" -> "CONTACTED".equals(lead.status()) || "ENROLLED".equals(lead.status());
            default -> false;
        };
    }

    private boolean isHighIntentLead(CrmLeadDto lead) {
        return "demo".equals(lead.source()) || "counseling".equals(lead.source()) || "chatbot".equals(lead.source());
    }

    private boolean isReminderToday(CrmLeadDto lead, LocalDate today) {
        if (!hasPendingFollowUp(lead)) {
            return false;
        }
        return lead.followUpAt().toLocalDate().isEqual(today);
    }

    private boolean isReminderOverdue(CrmLeadDto lead, LocalDate today) {
        if (!hasPendingFollowUp(lead)) {
            return false;
        }
        return lead.followUpAt().toLocalDate().isBefore(today);
    }

    private boolean isReminderInNext7Days(CrmLeadDto lead, LocalDate today) {
        if (!hasPendingFollowUp(lead)) {
            return false;
        }
        LocalDate followDate = lead.followUpAt().toLocalDate();
        return followDate.isAfter(today) && !followDate.isAfter(today.plusDays(7));
    }

    private boolean hasPendingFollowUp(CrmLeadDto lead) {
        if (lead.followUpAt() == null) {
            return false;
        }
        String status = StringUtils.hasText(lead.followUpStatus()) ? lead.followUpStatus() : "NONE";
        return !"COMPLETED".equalsIgnoreCase(status);
    }

    private boolean matchesDateRange(CrmLeadDto lead, LocalDateTime fromDateTime, LocalDateTime toDateTime) {
        if (lead.createdAt() == null) {
            return false;
        }
        if (fromDateTime != null && lead.createdAt().isBefore(fromDateTime)) {
            return false;
        }
        return toDateTime == null || !lead.createdAt().isAfter(toDateTime);
    }

    private boolean matchesAssignee(CrmLeadDto lead, String assignee) {
        if (!StringUtils.hasText(assignee) || "all".equals(assignee)) {
            return true;
        }
        return StringUtils.hasText(lead.assignee())
                && lead.assignee().toLowerCase(Locale.ROOT).contains(assignee);
    }

    private boolean matchesFollowUpStatus(CrmLeadDto lead, String followUpStatus) {
        if (!StringUtils.hasText(followUpStatus) || "all".equals(followUpStatus)) {
            return true;
        }
        return StringUtils.hasText(lead.followUpStatus())
                && lead.followUpStatus().equalsIgnoreCase(followUpStatus);
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String requireSupportedSource(String source) {
        String normalized = requireNonBlank(source, "Lead source is required").toLowerCase(Locale.ROOT);
        if (!List.of("contact", "demo", "counseling", "chatbot").contains(normalized)) {
            throw new BusinessException("Unsupported lead source", "UNSUPPORTED_SOURCE");
        }
        return normalized;
    }

    private void updateChatbotLeadStatus(Long sourceId, String status) {
        Long tenantId = TenantContext.requireTenantId();
        ChatbotQualifiedLead lead = chatbotQualifiedLeadRepository.findByIdAndTenantId(sourceId, tenantId)
                .orElseThrow(() -> new BusinessException("Lead not found", "LEAD_NOT_FOUND"));
        try {
            lead.setStatus(ChatbotQualifiedLead.Status.valueOf(status.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid chatbot lead status", "INVALID_STATUS");
        }
        chatbotQualifiedLeadRepository.save(lead);
    }

    private CrmLeadPipeline.FollowUpStatus parseFollowUpStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return CrmLeadPipeline.FollowUpStatus.NONE;
        }
        try {
            return CrmLeadPipeline.FollowUpStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid follow-up status", "INVALID_FOLLOW_UP_STATUS");
        }
    }

    private String assignee(CrmLeadPipeline pipeline) {
        return pipeline == null ? null : pipeline.getAssignee();
    }

    private String followUpStatus(CrmLeadPipeline pipeline) {
        return pipeline == null || pipeline.getFollowUpStatus() == null ? null : pipeline.getFollowUpStatus().name();
    }

    private LocalDateTime followUpAt(CrmLeadPipeline pipeline) {
        return pipeline == null ? null : pipeline.getFollowUpAt();
    }

    private String followUpNotes(CrmLeadPipeline pipeline) {
        return pipeline == null ? null : pipeline.getFollowUpNotes();
    }

    private String key(String source, Long sourceId) {
        return source + "#" + sourceId;
    }

    private String requireNonBlank(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message, "INVALID_INPUT");
        }
        return value.trim();
    }

    private String safeValue(String value) {
        return StringUtils.hasText(value) ? value : "UNKNOWN";
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "" : value.format(CSV_DATE_FORMAT);
    }

    private String csv(String value) {
        String normalized = value == null ? "" : value.replace('"', '\'');
        return '"' + normalized + '"';
    }
}