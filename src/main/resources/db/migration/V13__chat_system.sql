-- ============================================================
-- V13: Chat System — Conversations, Messages, Friend Requests
-- ============================================================

-- ── Conversations ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS conversations (
    conversation_id     UUID PRIMARY KEY,
    type                VARCHAR(20)  NOT NULL,
    name                VARCHAR(255),
    related_entity_id   UUID,
    related_info        TEXT,

    -- Audit fields
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_at          TIMESTAMPTZ,
    updated_by          UUID,
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          UUID
);

CREATE INDEX IF NOT EXISTS idx_conversations_type
    ON conversations (type);

-- ── Conversation Participants ─────────────────────────────
CREATE TABLE IF NOT EXISTS conversation_participants (
    conversation_id     UUID         NOT NULL REFERENCES conversations (conversation_id) ON DELETE CASCADE,
    user_id             UUID         NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    joined_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_read_at        TIMESTAMPTZ,
    unread_count        INTEGER      NOT NULL DEFAULT 0,

    -- Audit fields
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_at          TIMESTAMPTZ,
    updated_by          UUID,
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          UUID,

    PRIMARY KEY (conversation_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_conversation_participants_user
    ON conversation_participants (user_id);

-- ── Chat Messages ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS chat_messages (
    message_id          UUID         PRIMARY KEY,
    conversation_id     UUID         NOT NULL REFERENCES conversations (conversation_id) ON DELETE CASCADE,
    sender_id           UUID         NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    content             TEXT         NOT NULL,
    sent_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    seen                BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Audit fields
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_at          TIMESTAMPTZ,
    updated_by          UUID,
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          UUID
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_conversation
    ON chat_messages (conversation_id, sent_at DESC);

-- ── Friend Requests ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS friend_requests (
    request_id          UUID         PRIMARY KEY,
    requester_id        UUID         NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    addressee_id        UUID         NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    status              VARCHAR(20)  NOT NULL,
    responded_at        TIMESTAMPTZ,

    -- Audit fields
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_at          TIMESTAMPTZ,
    updated_by          UUID,
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          UUID
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_friend_requests_pair
    ON friend_requests (requester_id, addressee_id);

CREATE INDEX IF NOT EXISTS idx_friend_requests_addressee_status
    ON friend_requests (addressee_id, status);

CREATE INDEX IF NOT EXISTS idx_friend_requests_requester
    ON friend_requests (requester_id);
