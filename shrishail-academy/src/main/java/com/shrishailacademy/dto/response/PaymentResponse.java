package com.shrishailacademy.dto.response;

import com.shrishailacademy.model.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Payment responses - prevents exposing full user/course entities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private Long courseId;
    private String courseTitle;
    private Long enrollmentId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private String transactionId;
    private String gatewayOrderId;
    private String gatewayPaymentId;
    private String receiptNumber;
    private String remarks;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PaymentResponse fromEntity(Payment payment) {
        if (payment == null)
            return null;
        return PaymentResponse.builder()
                .id(payment.getId())
                .userId(payment.getUser() != null ? payment.getUser().getId() : null)
                .userName(payment.getUser() != null ? payment.getUser().getName() : null)
                .userEmail(payment.getUser() != null ? payment.getUser().getEmail() : null)
                .courseId(payment.getCourse() != null ? payment.getCourse().getId() : null)
                .courseTitle(payment.getCourse() != null ? payment.getCourse().getTitle() : null)
                .enrollmentId(payment.getEnrollment() != null ? payment.getEnrollment().getId() : null)
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null)
                .status(payment.getStatus() != null ? payment.getStatus().name() : null)
                .transactionId(payment.getTransactionId())
                .gatewayOrderId(payment.getGatewayOrderId())
                .gatewayPaymentId(payment.getGatewayPaymentId())
                .receiptNumber(payment.getReceiptNumber())
                .remarks(payment.getRemarks())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
