package com.shrishailacademy.service;

import com.shrishailacademy.dto.ContactRequest;
import com.shrishailacademy.exception.BusinessException;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.ContactMessage;
import com.shrishailacademy.repository.ContactMessageRepository;
import com.shrishailacademy.tenant.TenantContext;
import com.shrishailacademy.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    private final ContactMessageRepository contactRepo;
    private final TenantService tenantService;

    public ContactService(ContactMessageRepository contactRepo, TenantService tenantService) {
        this.contactRepo = contactRepo;
        this.tenantService = tenantService;
    }

    @Transactional
    public ContactMessage submitMessage(ContactRequest request) {
        ContactMessage msg = new ContactMessage();
        msg.setTenant(tenantService.requireCurrentTenant());
        msg.setName(InputSanitizer.sanitizeAndTruncate(request.getName(), 100));
        msg.setEmail(InputSanitizer.sanitizeEmailAndTruncate(request.getEmail(), 100));
        msg.setPhone(InputSanitizer.sanitizeAndTruncateNullable(request.getPhone(), 20));
        msg.setSubject(InputSanitizer.sanitizeAndTruncate(request.getSubject(), 200));
        msg.setMessage(InputSanitizer.sanitizeAndTruncate(request.getMessage(), 2000));
        msg.setStatus(ContactMessage.Status.NEW);

        ContactMessage saved = contactRepo.save(msg);
        log.info("CONTACT_SUBMITTED: from='{}' email='{}'", saved.getName(), saved.getEmail());
        return saved;
    }

    public List<ContactMessage> getAllMessages() {
        Long tenantId = TenantContext.requireTenantId();
        return contactRepo.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    public List<ContactMessage> getUnreadMessages() {
        Long tenantId = TenantContext.requireTenantId();
        return contactRepo.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, ContactMessage.Status.NEW);
    }

    @Transactional
    public ContactMessage updateMessageStatus(Long id, String status) {
        Long tenantId = TenantContext.requireTenantId();
        ContactMessage msg = contactRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ContactMessage", "id", id));

        try {
            msg.setStatus(ContactMessage.Status.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "Invalid status. Use: NEW, READ, REPLIED, ARCHIVED", "INVALID_STATUS");
        }

        ContactMessage saved = contactRepo.save(msg);
        log.info("CONTACT_STATUS_UPDATED: id={}, status={}", id, status);
        return saved;
    }

    public Map<String, Object> getStats() {
        Long tenantId = TenantContext.requireTenantId();
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", contactRepo.countByTenantId(tenantId));
        stats.put("unread", contactRepo.countByTenantIdAndStatus(tenantId, ContactMessage.Status.NEW));
        stats.put("read", contactRepo.countByTenantIdAndStatus(tenantId, ContactMessage.Status.READ));
        stats.put("replied", contactRepo.countByTenantIdAndStatus(tenantId, ContactMessage.Status.REPLIED));
        return stats;
    }
}
