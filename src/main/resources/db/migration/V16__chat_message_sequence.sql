-- Add message sequence to stabilize ordering under high throughput
ALTER TABLE chat_messages
    ADD COLUMN IF NOT EXISTS message_seq BIGSERIAL;

-- Backfill existing rows
UPDATE chat_messages
SET message_seq = nextval('chat_messages_message_seq_seq')
WHERE message_seq IS NULL;

ALTER TABLE chat_messages
    ALTER COLUMN message_seq SET NOT NULL;

-- Denormalize conversation last message metadata for faster chat list
ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS last_message_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS last_message_preview TEXT,
    ADD COLUMN IF NOT EXISTS last_message_sender_id UUID,
    ADD COLUMN IF NOT EXISTS last_message_seq BIGINT;

-- Backfill last_message_at for existing rows
UPDATE conversations
SET last_message_at = COALESCE(last_message_at, created_at)
WHERE last_message_at IS NULL;

-- Backfill last message metadata from chat_messages (message_seq primary)
UPDATE conversations c
SET last_message_at = sub.sent_at,
    last_message_preview = sub.content,
    last_message_sender_id = sub.sender_id,
    last_message_seq = sub.message_seq
FROM (
    SELECT DISTINCT ON (conversation_id)
           conversation_id,
           sent_at,
           sender_id,
           message_seq,
           LEFT(content, 200) AS content
    FROM chat_messages
    WHERE is_deleted = false
    ORDER BY conversation_id, message_seq DESC
) sub
WHERE c.conversation_id = sub.conversation_id;

-- Index to speed conversation list ordering
CREATE INDEX IF NOT EXISTS idx_conversations_last_message_at
    ON conversations (last_message_at DESC, last_message_seq DESC);

-- Index to speed participant lookup by conversation
CREATE INDEX IF NOT EXISTS idx_conversation_participants_conversation
    ON conversation_participants (conversation_id);

-- Index to speed participant lookup by user
CREATE INDEX IF NOT EXISTS idx_conversation_participants_user_conversation
    ON conversation_participants (user_id, conversation_id);

-- Composite index to speed cursor scans with stable ordering
CREATE INDEX IF NOT EXISTS idx_chat_messages_conversation_seq
    ON chat_messages (conversation_id, message_seq DESC);

-- Index by sender for user-based lookups
CREATE INDEX IF NOT EXISTS idx_chat_messages_sender
    ON chat_messages (sender_id);
