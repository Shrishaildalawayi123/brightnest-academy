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

CREATE TABLE IF NOT EXISTS chatbot_qualified_leads (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    session_id VARCHAR(80) NOT NULL,
    lead_name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    grade VARCHAR(30),
    board VARCHAR(50),
    subject_interest VARCHAR(100),
    user_intent_message VARCHAR(500),
    recommended_plan VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    INDEX idx_chatbot_qualified_tenant (tenant_id),
    INDEX idx_chatbot_qualified_session (session_id),
    INDEX idx_chatbot_qualified_status (status),
    INDEX idx_chatbot_qualified_created (created_at)
);

CREATE TABLE IF NOT EXISTS crm_lead_pipeline (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    source VARCHAR(30) NOT NULL,
    source_id BIGINT NOT NULL,
    assignee VARCHAR(120),
    follow_up_at TIMESTAMP NULL,
    follow_up_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
    follow_up_notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    UNIQUE KEY uk_crm_pipeline_tenant_source (tenant_id, source, source_id),
    INDEX idx_crm_pipeline_tenant (tenant_id),
    INDEX idx_crm_pipeline_assignee (assignee),
    INDEX idx_crm_pipeline_follow_up_at (follow_up_at)
);
