CREATE TABLE importacao_arquivo (
    id BIGSERIAL PRIMARY KEY,
    contexto_id BIGINT NOT NULL REFERENCES contexto(id),
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    nome_arquivo VARCHAR(255) NOT NULL,
    formato VARCHAR(10) NOT NULL CHECK (formato IN ('PDF', 'XLSX')),
    status VARCHAR(15) NOT NULL CHECK (status IN ('CONCLUIDA', 'DESFEITA')),
    total_lidos INTEGER NOT NULL CHECK (total_lidos >= 0),
    total_importados INTEGER NOT NULL CHECK (total_importados >= 0),
    total_duplicados INTEGER NOT NULL CHECK (total_duplicados >= 0),
    data_criacao TIMESTAMPTZ NOT NULL,
    data_desfeita TIMESTAMPTZ,
    CONSTRAINT importacao_arquivo_nome_ck CHECK (BTRIM(nome_arquivo) <> ''),
    CONSTRAINT importacao_arquivo_status_ck CHECK (
        (status = 'CONCLUIDA' AND data_desfeita IS NULL)
        OR (status = 'DESFEITA' AND data_desfeita IS NOT NULL)
    )
);

CREATE INDEX importacao_arquivo_contexto_data_idx
    ON importacao_arquivo (contexto_id, data_criacao DESC, id DESC);

ALTER TABLE transacao
    ADD COLUMN importacao_id BIGINT REFERENCES importacao_arquivo(id);

CREATE INDEX transacao_importacao_idx
    ON transacao (importacao_id)
    WHERE importacao_id IS NOT NULL;

INSERT INTO categoria (nome, tipo)
VALUES ('Outros', 'PADRAO');
