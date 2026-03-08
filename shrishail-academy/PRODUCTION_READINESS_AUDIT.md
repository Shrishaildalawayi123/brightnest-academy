// Controller accepts optional Idempotency-Key header
@PostMapping("/initiate")
public ResponseEntity<ApiResponse> initiatePayment(
@RequestHeader(name = "Idempotency-Key", required = false) String key
) { ... }

// Service checks for existing payment before creating
if (idempotencyKey != null) {
Optional<Payment> existing = paymentRepository.findByIdempotencyKeyAndTenantId(...);
if (existing.isPresent()) return existing.get(); // Safe retry
}
