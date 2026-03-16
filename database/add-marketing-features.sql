-- =====================================================
-- Marketing Features Schema Additions
-- Adds chatbot conversation logging and WhatsApp lead tracking
-- =====================================================

CREATE TABLE IF NOT EXISTS chatbot_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_message VARCHAR(500) NOT NULL,
    bot_response VARCHAR(2000) NOT NULL,
    `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chatbot_timestamp (`timestamp`)
);

CREATE TABLE IF NOT EXISTS whatsapp_leads (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source_page VARCHAR(200) NOT NULL,
    device_type VARCHAR(20) NOT NULL,
    INDEX idx_whatsapp_timestamp (`timestamp`),
    INDEX idx_whatsapp_source_page (source_page)
);
