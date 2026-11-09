CREATE TABLE convite_contexto (
    id BIGSERIAL PRIMARY KEY,
    contexto_id BIGINT NOT NULL REFERENCES contexto(id),
    usuario_remetente_id BIGINT NOT NULL REFERENCES usuario(id),
    email_convidado VARCHAR(120) NOT NULL,
    papel_proposto VARCHAR(25) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    status VARCHAR(15) NOT NULL,
    data_criacao TIMESTAMPTZ NOT NULL,
    data_expiracao TIMESTAMPTZ NOT NULL,
    data_resposta TIMESTAMPTZ,
    CONSTRAINT convite_contexto_token_hash_uk UNIQUE (token_hash),
    CONSTRAINT convite_contexto_papel_ck
        CHECK (papel_proposto IN ('COLABORADOR', 'DEPENDENTE')),
    CONSTRAINT convite_contexto_status_ck
        CHECK (status IN ('PENDENTE', 'ACEITO', 'RECUSADO', 'EXPIRADO', 'CANCELADO')),
    CONSTRAINT convite_contexto_datas_ck
        CHECK (data_expiracao > data_criacao)
);

CREATE UNIQUE INDEX convite_contexto_pendente_uk
    ON convite_contexto (contexto_id, LOWER(email_convidado))
    WHERE status = 'PENDENTE';

CREATE INDEX convite_contexto_email_idx
    ON convite_contexto (LOWER(email_convidado), data_criacao DESC);

CREATE INDEX contexto_participante_contexto_ativo_idx
    ON contexto_participante (contexto_id)
    WHERE data_saida IS NULL;
