-- Fix role column size to accommodate enum values
USE shrishail_academy;

ALTER TABLE users MODIFY COLUMN role VARCHAR(50) NOT NULL DEFAULT 'STUDENT';

-- Verify the change
DESCRIBE users;
