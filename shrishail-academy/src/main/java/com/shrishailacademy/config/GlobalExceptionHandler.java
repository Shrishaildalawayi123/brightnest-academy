package com.shrishailacademy.config;

import com.shrishailacademy.dto.ApiErrorDetails;
import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized exception handler for all REST controllers.
 * Maps domain exceptions to proper HTTP status codes and safe response bodies.
 * NEVER leaks stack traces or internal details to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationErrors(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        log.warn("Validation failed: {}", fieldErrors);
        String firstError = fieldErrors.values().iterator().next();
        ApiErrorDetails details = buildErrorDetails(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", request, fieldErrors);
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed: " + firstError, details));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse> handleResourceNotFound(ResourceNotFoundException ex,
            HttpServletRequest request) {
        log.info("Resource not found: {}", ex.getMessage());
        ApiErrorDetails details = buildErrorDetails(HttpStatus.NOT_FOUND, "NOT_FOUND", request, null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), details));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        log.info("Duplicate resource: {}", ex.getMessage());
        ApiErrorDetails details = buildErrorDetails(HttpStatus.CONFLICT, "DUPLICATE", request, null);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), details));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        ApiErrorDetails details = buildErrorDetails(HttpStatus.FORBIDDEN, "FORBIDDEN", request, null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage(), details));
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ApiResponse> handleInvalidState(InvalidStateTransitionException ex,
            HttpServletRequest request) {
        log.warn("Invalid state transition: {}", ex.getMessage());
        ApiErrorDetails details = buildErrorDetails(HttpStatus.CONFLICT, "INVALID_STATE", request, null);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), details));
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponse> handlePaymentError(PaymentException ex, HttpServletRequest request) {
        log.warn("Payment error: {}", ex.getMessage());
        ApiErrorDetails details = buildErrorDetails(HttpStatus.BAD_REQUEST, "PAYMENT_ERROR", request, null);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage(), details));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse> handleBusinessError(BusinessException ex, HttpServletRequest request) {
        log.warn("Business error [{}]: {}", ex.getErrorCode(), ex.getMessage());
        ApiErrorDetails details = buildErrorDetails(HttpStatus.BAD_REQUEST, ex.getErrorCode(), request, null);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage(), details));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        // Generic message to prevent user enumeration
        ApiErrorDetails details = buildErrorDetails(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", request, null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid email or password.", details));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        ApiErrorDetails details = buildErrorDetails(HttpStatus.BAD_REQUEST, "ILLEGAL_ARGUMENT", request, null);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage(), details));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        log.warn("Type mismatch: {}", ex.getMessage());
        ApiErrorDetails details = buildErrorDetails(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH", request, null);
        return ResponseEntity.badRequest().body(ApiResponse.error("Invalid parameter value.", details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleBadJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Unreadable request body: {}", ex.getMessage());
        ApiErrorDetails details = buildErrorDetails(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", request, null);
        return ResponseEntity.badRequest().body(ApiResponse.error("Malformed request body.", details));
    }

    @ExceptionHandler({ MaxUploadSizeExceededException.class, MultipartException.class })
    public ResponseEntity<ApiResponse> handleMultipartSize(Exception ex, HttpServletRequest request) {
        log.warn("Multipart upload error: {}", ex.getMessage());
        ApiErrorDetails details = buildErrorDetails(HttpStatus.BAD_REQUEST, "UPLOAD_ERROR", request, null);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Upload failed. Please check file size/type and try again.", details));
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse> handleOptimisticLock(
            org.springframework.orm.ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        ApiErrorDetails details = buildErrorDetails(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK", request, null);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("This record was modified by another request. Please refresh and try again.",
                        details));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        log.error("Unhandled runtime exception: {}", ex.getMessage(), ex);
        ApiErrorDetails details = buildErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", request, null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later.", details));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        ApiErrorDetails details = buildErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", request, null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later.", details));
    }

    private ApiErrorDetails buildErrorDetails(
            HttpStatus status,
            String code,
            HttpServletRequest request,
            Map<String, String> fieldErrors) {

        String requestId = MDC.get(REQUEST_ID_MDC_KEY);
        if (requestId == null || requestId.isBlank()) {
            requestId = request.getHeader(REQUEST_ID_HEADER);
        }

        return ApiErrorDetails.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .code(code)
                .path(request.getRequestURI())
                .requestId(requestId)
                .fieldErrors(fieldErrors == null || fieldErrors.isEmpty() ? null : fieldErrors)
                .build();
    }
}
