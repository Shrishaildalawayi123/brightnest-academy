package com.shrishailacademy.service;

import com.shrishailacademy.dto.ChatbotLeadEnrichmentRequest;
import com.shrishailacademy.dto.ChatbotRequest;
import com.shrishailacademy.model.ChatbotQualifiedLead;
import com.shrishailacademy.model.ChatbotMessage;
import com.shrishailacademy.tenant.TenantContext;
import com.shrishailacademy.repository.ChatbotMessageRepository;
import com.shrishailacademy.repository.ChatbotQualifiedLeadRepository;
import com.shrishailacademy.util.InputSanitizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FAQ-backed chatbot service for the public academy website.
 */
@Service
@Transactional(readOnly = true)
public class ChatbotService {

    private static final String WHATSAPP_URL =
            "https://wa.me/917204193980?text=Hello%20BrightNest%20Academy%2C%20I%20would%20like%20to%20know%20about%20tuition%20classes.";

        private static final Pattern GRADE_PATTERN = Pattern.compile("(?:grade|class|std|standard)?\\s*(1[0-2]|[1-9])(?:st|nd|rd|th)?", Pattern.CASE_INSENSITIVE);

    private static final List<KnowledgeEntry> KNOWLEDGE_BASE = List.of(
            new KnowledgeEntry(
                    "courses",
                    List.of("course", "courses", "subject", "subjects", "offer", "offering", "class", "classes"),
                    "We offer tuition in Mathematics, Science, English, Kannada, Hindi, Sanskrit, and German for CBSE, ICSE, and Karnataka State Board students in Bangalore.",
                    List.of("Do you teach ICSE?", "What grades do you teach?", "How do I book a demo class?")),
            new KnowledgeEntry(
                    "fees",
                    List.of("fee", "fees", "cost", "pricing", "price", "monthly", "tuition fee"),
                    "Fees depend on the subject, grade, class mode, and number of sessions per week. We share the exact fee after understanding the student's requirement. The demo class fee is INR 100, and it is adjustable against the first month's tuition or refundable within 30 days if you do not enroll.",
                    List.of("How do I book a demo class?", "Do you teach online?", "Talk to a human on WhatsApp")),
            new KnowledgeEntry(
                    "boards",
                    List.of("icse", "cbse", "state board", "karnataka board", "board"),
                    "Yes. BrightNest Academy supports ICSE, CBSE, and Karnataka State Board students with board-aligned lesson plans, revision support, and exam-focused practice.",
                    List.of("What courses do you offer?", "Do you provide online tuition?", "Do you teach Mathematics?")),
            new KnowledgeEntry(
                    "demo",
                    List.of("demo", "trial", "book", "booking", "schedule", "sample class"),
                    "You can book a demo class from the website demo page or contact us on WhatsApp. After you submit the form, our team usually reaches out within 24 hours to confirm the schedule.",
                    List.of("What are the fees?", "Do you teach ICSE?", "Talk to a human on WhatsApp")),
            new KnowledgeEntry(
                    "mode",
                    List.of("online", "offline", "google meet", "in-person", "mode", "remote"),
                    "We provide both online tuition through Google Meet and offline tuition at our Bangalore learning center in Kumaraswamy Layout.",
                    List.of("Where is BrightNest Academy located?", "What grades do you teach?", "Book a demo class")),
            new KnowledgeEntry(
                    "grades",
                    List.of("grade", "grades", "class 1", "class 10", "age group", "which grades"),
                    "We primarily teach students from Grade 1 to Grade 10, with personalized support for school curriculum, exam prep, and concept building.",
                    List.of("Do you teach ICSE?", "What courses do you offer?", "Book a demo class")),
            new KnowledgeEntry(
                    "maths",
                    List.of("math", "maths", "mathematics", "science tuition"),
                    "Yes. We provide Mathematics and Science coaching with concept-first teaching, worksheets, and exam practice for ICSE, CBSE, and Karnataka State Board students.",
                    List.of("Do you teach ICSE?", "What are the fees?", "Book a demo class")),
            new KnowledgeEntry(
                    "location",
                    List.of("location", "address", "bangalore", "bengaluru", "where", "timing", "hours"),
                    "BrightNest Academy is located at #662, 1st Floor, 67th Cross, Near Blossom School, Kumaraswamy Layout, Banashankari 1st Stage, Bangalore - 560078. We are open every day from 9:00 AM to 9:00 PM.",
                    List.of("Get directions", "Do you provide online tuition?", "Talk to a human on WhatsApp")),
            new KnowledgeEntry(
                    "human-help",
                    List.of("whatsapp", "human", "advisor", "call", "contact", "parent support"),
                    "A team member can help right away on WhatsApp. Tap the WhatsApp button and send: Hello BrightNest Academy, I would like to know about tuition classes.",
                    List.of("Talk to a human on WhatsApp", "Book a demo class", "What are the fees?"))
    );

    private final ChatbotMessageRepository chatbotMessageRepository;
        private final ChatbotQualifiedLeadRepository chatbotQualifiedLeadRepository;
        private final TenantService tenantService;
        private final ConcurrentHashMap<String, LocalDateTime> awaitingGradeSessions = new ConcurrentHashMap<>();

        public ChatbotService(
                        ChatbotMessageRepository chatbotMessageRepository,
                        ChatbotQualifiedLeadRepository chatbotQualifiedLeadRepository,
                        TenantService tenantService) {
        this.chatbotMessageRepository = chatbotMessageRepository;
                this.chatbotQualifiedLeadRepository = chatbotQualifiedLeadRepository;
                this.tenantService = tenantService;
    }

    @Transactional
    public Map<String, Object> respond(ChatbotRequest request) {
        String userMessage = InputSanitizer.sanitizeAndTruncate(request.getMessage(), 500);
                String sessionId = resolveSessionId(request.getSessionId());
                ChatbotReply reply = resolveReply(sessionId, userMessage);

        ChatbotMessage chatbotMessage = new ChatbotMessage();
        chatbotMessage.setUserMessage(userMessage);
        chatbotMessage.setBotResponse(reply.answer());
        chatbotMessageRepository.save(chatbotMessage);

        return Map.of(
                                "sessionId", sessionId,
                "reply", reply.answer(),
                "confidence", reply.confidence(),
                "suggestions", reply.suggestions(),
                                "qualifiedLeadCaptured", reply.qualifiedLeadCaptured(),
                "whatsappUrl", WHATSAPP_URL);
    }

        @Transactional
        public Map<String, Object> enrichLeadContact(ChatbotLeadEnrichmentRequest request) {
                Long tenantId = TenantContext.requireTenantId();
                String sessionId = resolveSessionId(request.sessionId());
                String name = InputSanitizer.sanitizeAndTruncateNullable(request.name(), 100);
                String email = InputSanitizer.sanitizeEmailAndTruncate(request.email(), 100);
                String phone = normalizePhone(request.phone());
                String studentClass = InputSanitizer.sanitizeAndTruncateNullable(request.studentClass(), 30);

                if (!StringUtils.hasText(name)
                                && !StringUtils.hasText(email)
                                && !StringUtils.hasText(phone)
                                && !StringUtils.hasText(studentClass)) {
                        return Map.of("updated", false, "reason", "No contact fields provided");
                }

                ChatbotQualifiedLead lead = chatbotQualifiedLeadRepository
                                .findFirstByTenantIdAndSessionIdOrderByCreatedAtDesc(tenantId, sessionId)
                                .orElse(null);

                if (lead == null) {
                        return Map.of("updated", false, "reason", "No qualified lead found for this session yet");
                }

                if (StringUtils.hasText(name)) {
                        lead.setLeadName(name);
                }
                if (StringUtils.hasText(email)) {
                        lead.setEmail(email);
                }
                if (StringUtils.hasText(phone)) {
                        lead.setPhone(phone);
                }
                if (StringUtils.hasText(studentClass)) {
                        lead.setGrade(studentClass);
                }
                chatbotQualifiedLeadRepository.save(lead);

                return Map.of(
                                "updated", true,
                                "leadId", lead.getId(),
                                "sessionId", sessionId);
        }

        private ChatbotReply resolveReply(String sessionId, String userMessage) {
        String normalized = userMessage == null ? "" : userMessage.toLowerCase(Locale.ROOT);
                boolean feeIntent = hasFeeIntent(normalized);
                Integer grade = extractGrade(normalized);

                if (feeIntent && grade == null) {
                        awaitingGradeSessions.put(sessionId, LocalDateTime.now());
                        return new ChatbotReply(
                                        "Absolutely. I can give a close fee estimate right now. Please share your child's grade (for example: Grade 6 or Class 9) and preferred mode (online/offline).",
                                        "high",
                                        List.of("Grade 5", "Grade 8", "Grade 10", "Online classes"),
                                        false);
                }

                if (isAwaitingGrade(sessionId) && grade == null) {
                        return new ChatbotReply(
                                        "Please share only the student's grade first (Grade 1 to Grade 10). I will then recommend the right program and next step.",
                                        "medium",
                                        List.of("Grade 4", "Grade 7", "Grade 9", "Talk to a human on WhatsApp"),
                                        false);
                }

                if (grade != null && (feeIntent || isAwaitingGrade(sessionId))) {
                        awaitingGradeSessions.remove(sessionId);
                        String board = extractBoard(normalized);
                        String subject = extractSubject(normalized);
                        String plan = buildGradeRecommendation(grade, subject, board);
                        captureQualifiedLead(sessionId, userMessage, grade, board, subject, plan);
                        return new ChatbotReply(
                                        plan + " If you share your phone number, our admission advisor can send an exact monthly fee and schedule a demo class.",
                                        "high",
                                        List.of("Book a demo class", "Talk to a human on WhatsApp", "Share fee for online classes"),
                                        true);
                }

        KnowledgeEntry bestMatch = null;
        int bestScore = 0;

        for (KnowledgeEntry entry : KNOWLEDGE_BASE) {
            int score = scoreEntry(normalized, entry);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = entry;
            }
        }

        if (bestMatch == null || bestScore < 2) {
            return new ChatbotReply(
                    "I can help with courses, boards, fees, demo booking, online classes, and Bangalore center details. If you want personal guidance, please connect with our team on WhatsApp.",
                    "low",
                                        List.of("What courses do you offer?", "What are the fees?", "Talk to a human on WhatsApp"),
                                        false);
        }

        String confidence = bestScore >= 6 ? "high" : "medium";
                return new ChatbotReply(bestMatch.answer(), confidence, bestMatch.suggestions(), false);
    }

        private boolean hasFeeIntent(String normalizedMessage) {
                return List.of("fee", "fees", "cost", "pricing", "price", "monthly", "tuition fee", "charges")
                                .stream()
                                .anyMatch(normalizedMessage::contains);
        }

        private Integer extractGrade(String normalizedMessage) {
                Matcher matcher = GRADE_PATTERN.matcher(normalizedMessage);
                if (!matcher.find()) {
                        return null;
                }
                try {
                        return Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException ex) {
                        return null;
                }
        }

        private String extractBoard(String normalizedMessage) {
                if (normalizedMessage.contains("icse")) {
                        return "ICSE";
                }
                if (normalizedMessage.contains("cbse")) {
                        return "CBSE";
                }
                if (normalizedMessage.contains("state")) {
                        return "Karnataka State Board";
                }
                return null;
        }

        private String extractSubject(String normalizedMessage) {
                if (normalizedMessage.contains("math")) {
                        return "Mathematics";
                }
                if (normalizedMessage.contains("science")) {
                        return "Science";
                }
                if (normalizedMessage.contains("english")) {
                        return "English";
                }
                if (normalizedMessage.contains("kannada")) {
                        return "Kannada";
                }
                if (normalizedMessage.contains("hindi")) {
                        return "Hindi";
                }
                if (normalizedMessage.contains("sanskrit")) {
                        return "Sanskrit";
                }
                if (normalizedMessage.contains("german")) {
                        return "German";
                }
                return null;
        }

        private String buildGradeRecommendation(int grade, String subject, String board) {
                String subjectText = StringUtils.hasText(subject) ? subject : "core subjects";
                String boardText = StringUtils.hasText(board) ? board : "CBSE/ICSE/State Board";

                if (grade <= 4) {
                        return "For Grade " + grade + ", we recommend our Foundation Program in " + subjectText
                                        + " with concept-building and parent progress updates. This plan is available for " + boardText + ".";
                }
                if (grade <= 8) {
                        return "For Grade " + grade + ", our Progress Track in " + subjectText
                                        + " focuses on school-aligned learning, weekly tests, and confidence building for " + boardText + ".";
                }
                return "For Grade " + grade + ", we recommend our Exam Accelerator in " + subjectText
                                + " with chapter-wise tests, revision strategy, and board-focused preparation for " + boardText + ".";
        }

        private void captureQualifiedLead(
                        String sessionId,
                        String userMessage,
                        int grade,
                        String board,
                        String subject,
                        String recommendation) {
                ChatbotQualifiedLead lead = new ChatbotQualifiedLead();
                lead.setTenant(tenantService.requireCurrentTenant());
                lead.setSessionId(sessionId);
                lead.setGrade("Grade " + grade);
                lead.setBoard(board);
                lead.setSubjectInterest(subject);
                lead.setUserIntentMessage(userMessage);
                lead.setRecommendedPlan(recommendation);
                lead.setStatus(ChatbotQualifiedLead.Status.NEW);
                chatbotQualifiedLeadRepository.save(lead);
        }

        private String resolveSessionId(String incomingSessionId) {
                String sessionId = InputSanitizer.sanitizeAndTruncateNullable(incomingSessionId, 80);
                if (!StringUtils.hasText(sessionId)) {
                        return UUID.randomUUID().toString();
                }
                return sessionId;
        }

        private boolean isAwaitingGrade(String sessionId) {
                LocalDateTime lastPromptAt = awaitingGradeSessions.get(sessionId);
                if (lastPromptAt == null) {
                        return false;
                }
                if (Duration.between(lastPromptAt, LocalDateTime.now()).toMinutes() > 30) {
                        awaitingGradeSessions.remove(sessionId);
                        return false;
                }
                return true;
        }

        private String normalizePhone(String phone) {
                String sanitized = InputSanitizer.sanitizeAndTruncateNullable(phone, 20);
                if (!StringUtils.hasText(sanitized)) {
                        return null;
                }
                String normalized = sanitized.replaceAll("[^0-9+]", "");
                return normalized.length() > 20 ? normalized.substring(0, 20) : normalized;
        }

    private int scoreEntry(String message, KnowledgeEntry entry) {
        int score = 0;
        for (String keyword : entry.keywords()) {
            if (message.contains(keyword)) {
                score += keyword.contains(" ") ? 4 : 2;
            }
        }
        return score;
    }

    private record KnowledgeEntry(String key, List<String> keywords, String answer, List<String> suggestions) {
    }

        private record ChatbotReply(String answer, String confidence, List<String> suggestions,
                        boolean qualifiedLeadCaptured) {
    }
}
