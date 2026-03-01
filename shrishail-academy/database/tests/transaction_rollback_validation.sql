-- Transaction rollback validation

START TRANSACTION;

INSERT INTO contact_messages (name, email, subject, message, status, created_at)
VALUES ('Rollback QA', 'rollback-qa@example.com', 'tx-test', 'rollback should remove this row', 'NEW', NOW());

SELECT COUNT(*) AS before_rollback_count
FROM contact_messages
WHERE email = 'rollback-qa@example.com';

ROLLBACK;

SELECT COUNT(*) AS after_rollback_count
FROM contact_messages
WHERE email = 'rollback-qa@example.com';
