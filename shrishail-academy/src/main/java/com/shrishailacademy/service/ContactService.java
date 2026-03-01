package com.shrishailacademy.service;

import com.shrishailacademy.dto.ContactRequest;
import com.shrishailacademy.exception.BusinessException;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.ContactMessage;
import com.shrishailacademy.repository.ContactMessageRepository;
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

    public ContactService(ContactMessageRepository contactRepo) {
        this.contactRepo = contactRepo;
    }

    @Transactional
    public ContactMessage submitMessage(ContactRequest request) {
        ContactMessage msg = new ContactMessage();
        msg.setName(request.getName());
        msg.setEmail(request.getEmail());
        msg.setPhone(request.getPhone());
        msg.setSubject(request.getSubject());
        msg.setMessage(request.getMessage());
        msg.setStatus(ContactMessage.Status.NEW);

        ContactMessage saved = contactRepo.save(msg);
        log.info("CONTACT_SUBMITTED: from='{}' email='{}'", request.getName(), request.getEmail());
        return saved;
    }

    public List<ContactMessage> getAllMessages() {
        return contactRepo.findAllByOrderByCreatedAtDesc();
    }

    public List<ContactMessage> getUnreadMessages() {
        return contactRepo.findByStatusOrderByCreatedAtDesc(ContactMessage.Status.NEW);
    }

    @Transactional
    public ContactMessage updateMessageStatus(Long id, String status) {
        ContactMessage msg = contactRepo.findById(id)
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
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", contactRepo.count());
        stats.put("unread", contactRepo.countByStatus(ContactMessage.Status.NEW));
        stats.put("read", contactRepo.countByStatus(ContactMessage.Status.READ));
        stats.put("replied", contactRepo.countByStatus(ContactMessage.Status.REPLIED));
        return stats;
    }
}
