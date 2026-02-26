package com.shrishailacademy.repository;

import com.shrishailacademy.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUserId(Long userId);

    List<Payment> findByCourseId(Long courseId);

    List<Payment> findByUserIdAndCourseId(Long userId, Long courseId);

    List<Payment> findByUserIdAndStatus(Long userId, Payment.Status status);

    List<Payment> findByStatus(Payment.Status status);

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByReceiptNumber(String receiptNumber);

    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

    boolean existsByUserIdAndCourseIdAndStatus(Long userId, Long courseId, Payment.Status status);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'SUCCESS'")
    Double getTotalRevenue();

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.user.id = :userId AND p.status = 'SUCCESS'")
    Double getTotalPaidByUser(@Param("userId") Long userId);

    @Query("SELECT p.paymentMethod, COUNT(p), SUM(p.amount) FROM Payment p WHERE p.status = 'SUCCESS' GROUP BY p.paymentMethod")
    List<Object[]> getPaymentMethodStats();

    long countByStatus(Payment.Status status);
}
