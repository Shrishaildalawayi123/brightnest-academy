package com.shrishailacademy.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull(message = "Course ID is required")
    private Long courseId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private Double amount;

    private String paymentMethod; // UPI, BANK_TRANSFER, CASH, CARD, ONLINE_PORTAL

    private String transactionId;

    private String remarks;
}
