-- Add expo push token to users and indexes to speed up chat cursors
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS expo_push_token VARCHAR(255);

-- Index to speed up queries ordering by sent_at per conversation
CREATE INDEX IF NOT EXISTS idx_chat_messages_conversation_sent_at
    ON chat_messages(conversation_id, sent_at DESC);

-- Optional single-column index for queries only by sent_at
CREATE INDEX IF NOT EXISTS idx_chat_messages_sent_at
    ON chat_messages(sent_at DESC);
