package com.shrishailacademy.controller;

import com.shrishailacademy.dto.WhatsappLeadRequest;
import com.shrishailacademy.service.WhatsappLeadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/whatsapp-leads", "/api/v1/whatsapp-leads"})
public class WhatsappLeadController {

    private final WhatsappLeadService whatsappLeadService;

    public WhatsappLeadController(WhatsappLeadService whatsappLeadService) {
        this.whatsappLeadService = whatsappLeadService;
    }

    @PostMapping
    public ResponseEntity<Void> captureLead(
            @Valid @RequestBody(required = false) WhatsappLeadRequest request,
            HttpServletRequest httpServletRequest) {
        whatsappLeadService.captureLead(request, httpServletRequest);
        return ResponseEntity.noContent().build();
    }
}
