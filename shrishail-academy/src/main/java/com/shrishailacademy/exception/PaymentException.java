package com.shrishailacademy.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown for payment-specific errors (invalid state transition, amount
 * mismatch, etc.)
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PaymentException extends BusinessException {

    private final String paymentId;

    public PaymentException(String message) {
        super("PAYMENT_ERROR", message);
        this.paymentId = null;
    }

    public PaymentException(String message, String paymentId) {
        super("PAYMENT_ERROR", message);
        this.paymentId = paymentId;
    }

    public String getPaymentId() {
        return paymentId;
    }
}
