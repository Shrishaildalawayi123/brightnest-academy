package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.Notification;
import com.shrishailacademy.repository.NotificationRepository;
import com.shrishailacademy.tenant.TenantContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({ "/api/notifications", "/api/v1/notifications" })
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final com.shrishailacademy.repository.UserRepository userRepository;

    public NotificationController(NotificationRepository notificationRepository,
            com.shrishailacademy.repository.UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
        public ResponseEntity<List<Notification>> getMyNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = resolveUserId(userDetails, tenantId);
        int boundedSize = Math.max(1, Math.min(size, 100));
        return ResponseEntity.ok(
            notificationRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(
                userId,
                tenantId,
                PageRequest.of(Math.max(page, 0), boundedSize, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<Notification>> getMyNotificationsForStudent(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return getMyNotifications(userDetails, page, size);
    }

    @GetMapping("/unread")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<Notification>> getUnreadNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = resolveUserId(userDetails, tenantId);
        int boundedSize = Math.max(1, Math.min(size, 100));
        return ResponseEntity.ok(
            notificationRepository.findByUserIdAndTenantIdAndReadFalseOrderByCreatedAtDesc(
                userId,
                tenantId,
                PageRequest.of(Math.max(page, 0), boundedSize, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = resolveUserId(userDetails, tenantId);
        long count = notificationRepository.countByUserIdAndTenantIdAndReadFalse(userId, tenantId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse> markAsRead(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long tenantId = TenantContext.requireTenantId();
        Notification notification = notificationRepository.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));

        Long userId = resolveUserId(userDetails, tenantId);

        if (!notification.getUser().getId().equals(userId) || !notification.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("Notification", "id", id);
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read"));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse> markAsReadPatch(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return markAsRead(id, userDetails);
    }

    private Long resolveUserId(UserDetails userDetails, Long tenantId) {
        return userRepository.findByEmailAndTenantId(userDetails.getUsername(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userDetails.getUsername()))
                .getId();
    }
}
