ALTER TABLE mail_messages ADD COLUMN conversation_id VARCHAR(36);
CREATE INDEX idx_mail_messages_conversation ON mail_messages (conversation_id, sent_at);
