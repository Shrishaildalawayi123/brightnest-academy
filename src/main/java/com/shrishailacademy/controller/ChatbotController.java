package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.dto.ChatbotLeadEnrichmentRequest;
import com.shrishailacademy.dto.ChatbotRequest;
import com.shrishailacademy.service.ChatbotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/chatbot", "/api/v1/chatbot"})
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/messages")
    public ResponseEntity<ApiResponse> respond(@Valid @RequestBody ChatbotRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Chatbot response generated",
                chatbotService.respond(request)));
    }

    @PostMapping("/leads/enrich")
    public ResponseEntity<ApiResponse> enrichLead(@Valid @RequestBody ChatbotLeadEnrichmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Chatbot lead enrichment processed",
                chatbotService.enrichLeadContact(request)));
    }
}
