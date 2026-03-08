-- Fix authentication data for testing
USE shrishail_academy;

-- Ensure tenant exists
INSERT INTO tenants (id, tenant_key, name, created_at, updated_at) 
VALUES (1, 'default', 'default', NOW(), NOW())
ON DUPLICATE KEY UPDATE name='default';

-- Update existing admin users to be verified
UPDATE users SET email_verified = 1 WHERE role = 'ADMIN';

-- Reset known admin accounts to a known password (admin123)
-- BCrypt generated on 2026-03-08 via BcryptHashGenerator
SET @admin_hash = '$2a$10$N5/3W2cMFcUtqVhYrNBj7O51PQs6A0ziazfkDB3XwTa.NNDzOaSSa';

UPDATE users
SET password = @admin_hash,
    email_verified = 1,
    failed_login_attempts = 0,
    locked_until = NULL,
    tenant_id = 1,
    role = 'ADMIN'
WHERE email IN ('admin@brightnest.com', 'admin@example.com');

-- Create test admin with known credentials (password: admin123)
-- Using BCrypt hash for "admin123": $2a$10$N5/3W2cMFcUtqVhYrNBj7O51PQs6A0ziazfkDB3XwTa.NNDzOaSSa
INSERT INTO users (tenant_id, name, email, password, phone, role, email_verified, failed_login_attempts, created_at, updated_at)
VALUES (1, 'Test Admin', 'testadmin@test.com', '$2a$10$N5/3W2cMFcUtqVhYrNBj7O51PQs6A0ziazfkDB3XwTa.NNDzOaSSa', '+91 99999 88888', 'ADMIN', 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
  password = '$2a$10$N5/3W2cMFcUtqVhYrNBj7O51PQs6A0ziazfkDB3XwTa.NNDzOaSSa',
  email_verified = 1,
  failed_login_attempts = 0,
  locked_until = NULL,
  tenant_id = 1;

-- Verify all users have tenant_id = 1
UPDATE users SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;

-- Display results
SELECT id, name, email, role, tenant_id, CAST(email_verified AS UNSIGNED) as verified
FROM users 
WHERE role = 'ADMIN' OR email LIKE '%test%'
ORDER BY id;
