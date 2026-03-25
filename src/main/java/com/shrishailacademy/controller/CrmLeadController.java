package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.dto.CrmLeadPipelineUpdateRequest;
import com.shrishailacademy.service.CrmLeadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping({"/api/admin/leads", "/api/v1/admin/leads"})
@PreAuthorize("hasRole('ADMIN')")
public class CrmLeadController {

    private final CrmLeadService crmLeadService;

    public CrmLeadController(CrmLeadService crmLeadService) {
        this.crmLeadService = crmLeadService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getLeads(
            @RequestParam(required = false, defaultValue = "all") String source,
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false, defaultValue = "all") String followUpStatus) {
        return ResponseEntity.ok(ApiResponse.success(
                "Leads retrieved",
                crmLeadService.getLeads(source, status, q, fromDate, toDate, assignee, followUpStatus)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse> getLeadStats() {
        return ResponseEntity.ok(ApiResponse.success("Lead stats retrieved", crmLeadService.getStats()));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> exportLeads(
            @RequestParam(required = false, defaultValue = "all") String source,
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false, defaultValue = "all") String followUpStatus) {
        String csv = crmLeadService.exportCsv(source, status, q, fromDate, toDate, assignee, followUpStatus);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=crm-leads.csv")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }

    @PutMapping("/{source}/{sourceId}/status")
    public ResponseEntity<ApiResponse> updateLeadStatus(
            @PathVariable String source,
            @PathVariable Long sourceId,
            @RequestParam String status) {
        crmLeadService.updateLeadStatus(source, sourceId, status);
        return ResponseEntity.ok(ApiResponse.success("Lead status updated"));
    }

    @PutMapping("/{source}/{sourceId}/pipeline")
    public ResponseEntity<ApiResponse> updateLeadPipeline(
            @PathVariable String source,
            @PathVariable Long sourceId,
            @Valid @RequestBody CrmLeadPipelineUpdateRequest request) {
        crmLeadService.updateLeadPipeline(source, sourceId, request);
        return ResponseEntity.ok(ApiResponse.success("Lead pipeline updated"));
    }
}