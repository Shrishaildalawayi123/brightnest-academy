package com.shrishailacademy.service;

import com.shrishailacademy.dto.ContactRequest;
import com.shrishailacademy.exception.BusinessException;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.ContactMessage;
import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.repository.ContactMessageRepository;
import com.shrishailacademy.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    private static final Long TENANT_ID = 1L;

    @Mock
    private ContactMessageRepository contactRepo;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private ContactService contactService;

    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT_ID, "test");
        testTenant = new Tenant();
        testTenant.setId(TENANT_ID);
        testTenant.setTenantKey("test");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void submitMessageShouldSanitizeAndSave() {
        ContactRequest request = new ContactRequest();
        request.setName("<script>alert(1)</script> John");
        request.setEmail("john@example.com");
        request.setPhone("1234567890");
        request.setSubject("Help");
        request.setMessage("I need help with courses");

        when(tenantService.requireCurrentTenant()).thenReturn(testTenant);
        when(contactRepo.save(any(ContactMessage.class))).thenAnswer(inv -> {
            ContactMessage m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        ContactMessage saved = contactService.submitMessage(request);

        assertFalse(saved.getName().contains("<script>"));
        assertEquals(ContactMessage.Status.NEW, saved.getStatus());
        verify(contactRepo).save(any(ContactMessage.class));
    }

    @Test
    void getAllMessagesShouldReturnOrderedList() {
        ContactMessage m1 = new ContactMessage();
        m1.setId(1L);
        ContactMessage m2 = new ContactMessage();
        m2.setId(2L);

        when(contactRepo.findByTenantIdOrderByCreatedAtDesc(TENANT_ID)).thenReturn(List.of(m2, m1));

        List<ContactMessage> result = contactService.getAllMessages();

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getId());
    }

    @Test
    void getUnreadMessagesShouldFilterByNewStatus() {
        when(contactRepo.findByTenantIdAndStatusOrderByCreatedAtDesc(TENANT_ID, ContactMessage.Status.NEW))
                .thenReturn(List.of(new ContactMessage()));

        List<ContactMessage> result = contactService.getUnreadMessages();

        assertEquals(1, result.size());
        verify(contactRepo).findByTenantIdAndStatusOrderByCreatedAtDesc(TENANT_ID, ContactMessage.Status.NEW);
    }

    @Test
    void updateMessageStatusShouldUpdateValidStatus() {
        ContactMessage msg = new ContactMessage();
        msg.setId(1L);
        msg.setStatus(ContactMessage.Status.NEW);

        when(contactRepo.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(msg));
        when(contactRepo.save(any(ContactMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        ContactMessage updated = contactService.updateMessageStatus(1L, "READ");

        assertEquals(ContactMessage.Status.READ, updated.getStatus());
    }

    @Test
    void updateMessageStatusShouldThrowOnInvalidStatus() {
        ContactMessage msg = new ContactMessage();
        msg.setId(1L);
        when(contactRepo.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(msg));

        assertThrows(BusinessException.class,
                () -> contactService.updateMessageStatus(1L, "INVALID"));
    }

    @Test
    void updateMessageStatusShouldThrowWhenNotFound() {
        when(contactRepo.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> contactService.updateMessageStatus(99L, "READ"));
    }

    @Test
    void getStatsShouldReturnCorrectCounts() {
        when(contactRepo.countByTenantId(TENANT_ID)).thenReturn(10L);
        when(contactRepo.countByTenantIdAndStatus(TENANT_ID, ContactMessage.Status.NEW)).thenReturn(5L);
        when(contactRepo.countByTenantIdAndStatus(TENANT_ID, ContactMessage.Status.READ)).thenReturn(3L);
        when(contactRepo.countByTenantIdAndStatus(TENANT_ID, ContactMessage.Status.REPLIED)).thenReturn(2L);

        Map<String, Object> stats = contactService.getStats();

        assertEquals(10L, stats.get("total"));
        assertEquals(5L, stats.get("unread"));
        assertEquals(3L, stats.get("read"));
        assertEquals(2L, stats.get("replied"));
    }
}
