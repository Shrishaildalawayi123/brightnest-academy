package com.shrishailacademy.repository;

import com.shrishailacademy.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUserId(Long userId);

    List<Payment> findByUserIdAndTenantId(Long userId, Long tenantId);

    List<Payment> findByCourseId(Long courseId);

    List<Payment> findByCourseIdAndTenantId(Long courseId, Long tenantId);

    List<Payment> findByUserIdAndCourseId(Long userId, Long courseId);

    List<Payment> findByUserIdAndCourseIdAndTenantId(Long userId, Long courseId, Long tenantId);

    List<Payment> findByUserIdAndStatus(Long userId, Payment.Status status);

    List<Payment> findByStatus(Payment.Status status);

    List<Payment> findAllByTenantId(Long tenantId);

    Page<Payment> findAllByTenantId(Long tenantId, Pageable pageable);

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByTransactionIdAndTenantId(String transactionId, Long tenantId);

    Optional<Payment> findByReceiptNumber(String receiptNumber);

    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

    boolean existsByUserIdAndCourseIdAndStatus(Long userId, Long courseId, Payment.Status status);

    boolean existsByUserIdAndCourseIdAndTenantIdAndStatus(Long userId, Long courseId, Long tenantId,
            Payment.Status status);

    Optional<Payment> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'SUCCESS'")
    BigDecimal getTotalRevenue();

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.tenant.id = :tenantId AND p.status = 'SUCCESS'")
    BigDecimal getTotalRevenueByTenant(@Param("tenantId") Long tenantId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.user.id = :userId AND p.status = 'SUCCESS'")
    BigDecimal getTotalPaidByUser(@Param("userId") Long userId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.tenant.id = :tenantId AND p.user.id = :userId AND p.status = 'SUCCESS'")
    BigDecimal getTotalPaidByUserInTenant(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Query("SELECT p.paymentMethod, COUNT(p), SUM(p.amount) FROM Payment p WHERE p.status = 'SUCCESS' GROUP BY p.paymentMethod")
    List<Object[]> getPaymentMethodStats();

    @Query("SELECT p.paymentMethod, COUNT(p), SUM(p.amount) FROM Payment p WHERE p.tenant.id = :tenantId AND p.status = 'SUCCESS' GROUP BY p.paymentMethod")
    List<Object[]> getPaymentMethodStatsByTenant(@Param("tenantId") Long tenantId);

    long countByStatus(Payment.Status status);

    long countByStatusAndTenantId(Payment.Status status, Long tenantId);
}
