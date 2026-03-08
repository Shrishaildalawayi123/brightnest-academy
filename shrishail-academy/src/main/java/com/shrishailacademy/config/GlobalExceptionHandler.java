package com.shrishailacademy.config;

import com.shrishailacademy.dto.ApiErrorResponse;
import com.shrishailacademy.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized exception handler for all REST controllers.
 * Maps domain exceptions to proper HTTP status codes and safe response bodies.
 * NEVER leaks stack traces or internal details to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                List<FieldError> errors = new ArrayList<>(ex.getBindingResult().getFieldErrors());
                errors.sort(Comparator.comparing(FieldError::getField));

                Map<String, String> fieldErrors = new LinkedHashMap<>();
                for (FieldError error : errors) {
                        fieldErrors.put(error.getField(), error.getDefaultMessage());
                }

                log.warn("Validation failed: {}", fieldErrors);
                String firstError = errors.isEmpty() ? "Validation failed" : errors.getFirst().getDefaultMessage();
                return ResponseEntity.badRequest().body(apiError(
                                HttpStatus.BAD_REQUEST,
                                "Validation Error",
                                firstError));
        }

        @ExceptionHandler(HandlerMethodValidationException.class)
        public ResponseEntity<ApiErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException ex,
                        HttpServletRequest request) {
                String message = ex.getAllErrors().isEmpty()
                                ? "Validation failed"
                                : ex.getAllErrors().getFirst().getDefaultMessage();
                log.warn("Handler method validation failed: {}", message);
                return ResponseEntity.badRequest()
                                .body(apiError(HttpStatus.BAD_REQUEST, "Validation Error", message));
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ApiErrorResponse> handleConstraintViolations(ConstraintViolationException ex,
                        HttpServletRequest request) {
                String message = ex.getConstraintViolations().stream()
                                .findFirst()
                                .map(v -> v.getMessage())
                                .orElse("Validation failed");
                log.warn("Constraint validation failed: {}", message);
                return ResponseEntity.badRequest()
                                .body(apiError(HttpStatus.BAD_REQUEST, "Validation Error", message));
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleResourceNotFound(ResourceNotFoundException ex,
                        HttpServletRequest request) {
                log.info("Resource not found: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(apiError(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage()));
        }

        @ExceptionHandler(DuplicateResourceException.class)
        public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateResourceException ex,
                        HttpServletRequest request) {
                log.info("Duplicate resource: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(apiError(HttpStatus.CONFLICT, "Conflict", ex.getMessage()));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex,
                        HttpServletRequest request) {
                log.warn("Access denied: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(apiError(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage()));
        }

        @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
        public ResponseEntity<ApiErrorResponse> handleSpringAccessDenied(
                        org.springframework.security.access.AccessDeniedException ex,
                        HttpServletRequest request) {
                log.warn("Spring security access denied: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(apiError(HttpStatus.FORBIDDEN, "Forbidden", "Access denied."));
        }

        @ExceptionHandler(InvalidStateTransitionException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidState(InvalidStateTransitionException ex,
                        HttpServletRequest request) {
                log.warn("Invalid state transition: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(apiError(HttpStatus.CONFLICT, "Conflict", ex.getMessage()));
        }

        @ExceptionHandler(PaymentException.class)
        public ResponseEntity<ApiErrorResponse> handlePaymentError(PaymentException ex, HttpServletRequest request) {
                log.warn("Payment error: {}", ex.getMessage());
                return ResponseEntity.badRequest()
                                .body(apiError(HttpStatus.BAD_REQUEST, "Payment Error", ex.getMessage()));
        }

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ApiErrorResponse> handleBusinessError(BusinessException ex, HttpServletRequest request) {
                log.warn("Business error [{}]: {}", ex.getErrorCode(), ex.getMessage());
                if ("ACCOUNT_LOCKED".equals(ex.getErrorCode())) {
                        return ResponseEntity.status(HttpStatus.LOCKED)
                                        .body(apiError(HttpStatus.LOCKED, "Locked", ex.getMessage()));
                }
                if ("EMAIL_NOT_VERIFIED".equals(ex.getErrorCode())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(apiError(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage()));
                }
                return ResponseEntity.badRequest()
                                .body(apiError(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage()));
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex,
                        HttpServletRequest request) {
                // Generic message to prevent user enumeration
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(apiError(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid email or password."));
        }

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex,
                        HttpServletRequest request) {
                log.warn("Authentication error: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(apiError(HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication required."));
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                        HttpServletRequest request) {
                log.warn("Illegal argument: {}", ex.getMessage());
                return ResponseEntity.badRequest()
                                .body(apiError(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage()));
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                        HttpServletRequest request) {
                log.warn("Type mismatch: {}", ex.getMessage());
                return ResponseEntity.badRequest()
                                .body(apiError(HttpStatus.BAD_REQUEST, "Bad Request", "Invalid parameter value."));
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiErrorResponse> handleBadJson(HttpMessageNotReadableException ex,
                        HttpServletRequest request) {
                log.warn("Unreadable request body: {}", ex.getMessage());
                return ResponseEntity.badRequest()
                                .body(apiError(HttpStatus.BAD_REQUEST, "Bad Request", "Malformed request body."));
        }

        @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
        public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex,
                        HttpServletRequest request) {
                log.warn("Unsupported media type: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                                .body(apiError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type",
                                                "Content type is not supported."));
        }

        @ExceptionHandler(MissingServletRequestParameterException.class)
        public ResponseEntity<ApiErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex,
                        HttpServletRequest request) {
                String message = "Missing required parameter: " + ex.getParameterName();
                log.warn("Missing request parameter: {}", message);
                return ResponseEntity.badRequest()
                                .body(apiError(HttpStatus.BAD_REQUEST, "Bad Request", message));
        }

        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                        HttpServletRequest request) {
                log.warn("Method not allowed: {}", ex.getMessage());
                HttpMethod[] allowedMethods = ex.getSupportedHttpMethods() == null
                                ? new HttpMethod[0]
                                : ex.getSupportedHttpMethods().toArray(new HttpMethod[0]);
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                                .allow(allowedMethods)
                                .body(apiError(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed",
                                                "Request method is not supported."));
        }

        @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleNoResourceFound(NoResourceFoundException ex,
                        HttpServletRequest request) {
                log.info("No resource found for path: {}", request.getRequestURI());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(apiError(HttpStatus.NOT_FOUND, "Not Found", "Resource not found."));
        }

        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex,
                        HttpServletRequest request) {
                log.warn("ResponseStatusException: {}", ex.getMessage());
                HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
                HttpStatus safeStatus = status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
                return ResponseEntity.status(safeStatus)
                                .body(apiError(safeStatus, safeStatus.getReasonPhrase(), ex.getReason()));
        }

        @ExceptionHandler({ MaxUploadSizeExceededException.class, MultipartException.class })
        public ResponseEntity<ApiErrorResponse> handleMultipartSize(Exception ex, HttpServletRequest request) {
                log.warn("Multipart upload error: {}", ex.getMessage());
                return ResponseEntity.badRequest()
                                .body(apiError(HttpStatus.BAD_REQUEST, "Bad Request",
                                                "Upload failed. Please check file size/type and try again."));
        }

        @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
        public ResponseEntity<ApiErrorResponse> handleOptimisticLock(
                        org.springframework.orm.ObjectOptimisticLockingFailureException ex,
                        HttpServletRequest request) {
                log.warn("Optimistic lock conflict: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(apiError(HttpStatus.CONFLICT, "Conflict",
                                                "This record was modified by another request. Please refresh and try again."));
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                        HttpServletRequest request) {
                log.warn("Data integrity violation: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(apiError(HttpStatus.CONFLICT, "Conflict",
                                                "Request violates data constraints."));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
                log.error("Unexpected error", ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(apiError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                                                "An unexpected error occurred. Please try again later."));
        }

        private ApiErrorResponse apiError(HttpStatus status, String error, String message) {
                return new ApiErrorResponse(java.time.Instant.now(), status.value(), error, message);
        }
}
