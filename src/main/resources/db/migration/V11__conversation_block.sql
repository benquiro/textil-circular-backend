-- Un participante puede pausar una conversación para ambas partes. Los mensajes ya enviados siguen
-- visibles; mientras blocked_at esté definido, ningún participante puede enviar uno nuevo.
ALTER TABLE conversation
    ADD COLUMN blocked_at timestamptz,
    ADD COLUMN blocked_by bigint REFERENCES user_account (user_id),
    -- El bloqueador y el instante siempre van juntos.
    ADD CONSTRAINT ck_conversation_block CHECK ((blocked_at IS NULL) = (blocked_by IS NULL));

CREATE INDEX ix_conversation_blocked_by ON conversation (blocked_by) WHERE blocked_by IS NOT NULL;
