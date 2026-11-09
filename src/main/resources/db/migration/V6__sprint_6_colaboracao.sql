CREATE TABLE dependente (
    id BIGSERIAL PRIMARY KEY,
    contexto_id BIGINT NOT NULL REFERENCES contexto(id),
    participante_id BIGINT UNIQUE REFERENCES contexto_participante(id),
    nome VARCHAR(120) NOT NULL,
    data_nascimento DATE NOT NULL,
    tipo VARCHAR(15) NOT NULL CHECK (tipo IN ('PASSIVO', 'LEITURA')),
    data_criacao TIMESTAMPTZ NOT NULL,
    data_atualizacao TIMESTAMPTZ NOT NULL,
    data_exclusao TIMESTAMPTZ,
    CONSTRAINT dependente_leitura_participante_ck CHECK (
        tipo = 'LEITURA' OR participante_id IS NULL
    )
);

CREATE INDEX dependente_contexto_idx
    ON dependente (contexto_id)
    WHERE data_exclusao IS NULL;

ALTER TABLE convite_contexto
    ADD COLUMN dependente_id BIGINT REFERENCES dependente(id);

ALTER TABLE convite_contexto
    ADD CONSTRAINT convite_contexto_dependente_ck CHECK (
        (papel_proposto = 'DEPENDENTE' AND dependente_id IS NOT NULL)
        OR (papel_proposto <> 'DEPENDENTE' AND dependente_id IS NULL)
    );

CREATE INDEX convite_contexto_dependente_idx
    ON convite_contexto (dependente_id)
    WHERE dependente_id IS NOT NULL;
