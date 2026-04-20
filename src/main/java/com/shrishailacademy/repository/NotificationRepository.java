package com.shrishailacademy.repository;

import com.shrishailacademy.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdAndTenantIdOrderByCreatedAtDesc(Long userId, Long tenantId);

    List<Notification> findByUserIdAndTenantIdOrderByCreatedAtDesc(Long userId, Long tenantId, Pageable pageable);

    List<Notification> findByUserIdAndTenantIdAndReadFalseOrderByCreatedAtDesc(Long userId, Long tenantId);

    List<Notification> findByUserIdAndTenantIdAndReadFalseOrderByCreatedAtDesc(Long userId, Long tenantId,
            Pageable pageable);

    java.util.Optional<Notification> findByIdAndTenant_Id(Long id, Long tenantId);

    long countByUserIdAndTenantIdAndReadFalse(Long userId, Long tenantId);
}
