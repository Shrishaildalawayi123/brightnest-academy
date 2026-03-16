package com.shrishailacademy.service;

import com.shrishailacademy.dto.ChatbotRequest;
import com.shrishailacademy.model.ChatbotMessage;
import com.shrishailacademy.repository.ChatbotMessageRepository;
import com.shrishailacademy.util.InputSanitizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * FAQ-backed chatbot service for the public academy website.
 */
@Service
@Transactional(readOnly = true)
public class ChatbotService {

    private static final String WHATSAPP_URL =
            "https://wa.me/917204193980?text=Hello%20BrightNest%20Academy%2C%20I%20would%20like%20to%20know%20about%20tuition%20classes.";

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

    public ChatbotService(ChatbotMessageRepository chatbotMessageRepository) {
        this.chatbotMessageRepository = chatbotMessageRepository;
    }

    @Transactional
    public Map<String, Object> respond(ChatbotRequest request) {
        String userMessage = InputSanitizer.sanitizeAndTruncate(request.getMessage(), 500);
        ChatbotReply reply = resolveReply(userMessage);

        ChatbotMessage chatbotMessage = new ChatbotMessage();
        chatbotMessage.setUserMessage(userMessage);
        chatbotMessage.setBotResponse(reply.answer());
        chatbotMessageRepository.save(chatbotMessage);

        return Map.of(
                "reply", reply.answer(),
                "confidence", reply.confidence(),
                "suggestions", reply.suggestions(),
                "whatsappUrl", WHATSAPP_URL);
    }

    private ChatbotReply resolveReply(String userMessage) {
        String normalized = userMessage == null ? "" : userMessage.toLowerCase(Locale.ROOT);
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
                    List.of("What courses do you offer?", "What are the fees?", "Talk to a human on WhatsApp"));
        }

        String confidence = bestScore >= 6 ? "high" : "medium";
        return new ChatbotReply(bestMatch.answer(), confidence, bestMatch.suggestions());
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

    private record ChatbotReply(String answer, String confidence, List<String> suggestions) {
    }
}
