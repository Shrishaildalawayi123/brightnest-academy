package com.shrishailacademy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Payment Entity - Tracks fee payments for course enrollments
 */
@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({ "enrollments", "password", "hibernateLazyInitializer" })
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnoreProperties({ "enrollments", "hibernateLazyInitializer" })
    private Course course;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "enrollment_id")
    @JsonIgnoreProperties({ "user", "course", "hibernateLazyInitializer" })
    private Enrollment enrollment;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "payment_method", length = 30)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod = PaymentMethod.UPI;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @Column(name = "transaction_id", length = 100, unique = true)
    private String transactionId;

    @Column(name = "gateway_order_id", length = 100)
    private String gatewayOrderId;

    @Column(name = "gateway_payment_id", length = 100)
    private String gatewayPaymentId;

    @Column(name = "receipt_number", length = 50, unique = true)
    private String receiptNumber;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Payment Status
     */
    public enum Status {
        PENDING,
        SUCCESS,
        FAILED,
        REFUNDED
    }

    /**
     * Payment Method
     */
    public enum PaymentMethod {
        UPI,
        BANK_TRANSFER,
        CASH,
        CARD,
        ONLINE_PORTAL
    }
}
