package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.Notification;
import com.shrishailacademy.repository.NotificationRepository;
import com.shrishailacademy.tenant.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/notifications", "/api/v1/notifications"})
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final com.shrishailacademy.repository.UserRepository userRepository;

    public NotificationController(NotificationRepository notificationRepository,
            com.shrishailacademy.repository.UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = resolveUserId(userDetails, tenantId);
        return ResponseEntity.ok(
                notificationRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(userId, tenantId));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = resolveUserId(userDetails, tenantId);
        return ResponseEntity.ok(
                notificationRepository.findByUserIdAndTenantIdAndReadFalseOrderByCreatedAtDesc(userId, tenantId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = resolveUserId(userDetails, tenantId);
        long count = notificationRepository.countByUserIdAndTenantIdAndReadFalse(userId, tenantId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse> markAsRead(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));

        Long tenantId = TenantContext.requireTenantId();
        Long userId = resolveUserId(userDetails, tenantId);

        if (!notification.getUser().getId().equals(userId) || !notification.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("Notification", "id", id);
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read"));
    }

    private Long resolveUserId(UserDetails userDetails, Long tenantId) {
        return userRepository.findByEmailAndTenantId(userDetails.getUsername(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userDetails.getUsername()))
                .getId();
    }
}



