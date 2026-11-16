-- A participant can pause a conversation for both sides. Messages already sent stay visible;
-- while blocked_at is set, neither participant can send a new one.
ALTER TABLE conversation
    ADD COLUMN blocked_at timestamptz,
    ADD COLUMN blocked_by bigint REFERENCES user_account (user_id),
    -- The blocker and the instant always travel together.
    ADD CONSTRAINT ck_conversation_block CHECK ((blocked_at IS NULL) = (blocked_by IS NULL));

CREATE INDEX ix_conversation_blocked_by ON conversation (blocked_by) WHERE blocked_by IS NOT NULL;
