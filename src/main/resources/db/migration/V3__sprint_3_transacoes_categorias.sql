CREATE TABLE categoria (
    id BIGSERIAL PRIMARY KEY,
    contexto_id BIGINT REFERENCES contexto(id),
    nome VARCHAR(60) NOT NULL,
    tipo VARCHAR(15) NOT NULL,
    data_exclusao TIMESTAMPTZ,
    CONSTRAINT categoria_nome_ck CHECK (BTRIM(nome) <> ''),
    CONSTRAINT categoria_tipo_ck CHECK (tipo IN ('PADRAO', 'PERSONALIZADA')),
    CONSTRAINT categoria_escopo_ck CHECK (
        (tipo = 'PADRAO' AND contexto_id IS NULL)
        OR (tipo = 'PERSONALIZADA' AND contexto_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX categoria_padrao_nome_uk
    ON categoria (LOWER(nome))
    WHERE tipo = 'PADRAO';

CREATE UNIQUE INDEX categoria_personalizada_ativa_nome_uk
    ON categoria (contexto_id, LOWER(nome))
    WHERE tipo = 'PERSONALIZADA' AND data_exclusao IS NULL;

INSERT INTO categoria (nome, tipo)
VALUES
    ('Alimentação', 'PADRAO'),
    ('Transporte', 'PADRAO'),
    ('Saúde', 'PADRAO'),
    ('Lazer', 'PADRAO'),
    ('Salário', 'PADRAO');

CREATE TABLE transacao (
    id BIGSERIAL PRIMARY KEY,
    contexto_id BIGINT NOT NULL REFERENCES contexto(id),
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    categoria_id BIGINT NOT NULL REFERENCES categoria(id),
    tipo VARCHAR(10) NOT NULL,
    valor NUMERIC(12, 2) NOT NULL,
    data DATE NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    data_criacao TIMESTAMPTZ NOT NULL,
    data_atualizacao TIMESTAMPTZ NOT NULL,
    CONSTRAINT transacao_tipo_ck CHECK (tipo IN ('ENTRADA', 'SAIDA')),
    CONSTRAINT transacao_valor_ck CHECK (valor > 0),
    CONSTRAINT transacao_descricao_ck CHECK (BTRIM(descricao) <> '')
);

CREATE INDEX transacao_contexto_data_idx
    ON transacao (contexto_id, data DESC, id DESC);

CREATE INDEX transacao_contexto_categoria_idx
    ON transacao (contexto_id, categoria_id);

CREATE OR REPLACE FUNCTION validar_categoria_da_transacao()
RETURNS TRIGGER AS $$
DECLARE
    categoria_tipo VARCHAR(15);
    categoria_contexto_id BIGINT;
    categoria_data_exclusao TIMESTAMPTZ;
BEGIN
    SELECT tipo, contexto_id, data_exclusao
      INTO categoria_tipo, categoria_contexto_id, categoria_data_exclusao
      FROM categoria
     WHERE id = NEW.categoria_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Categoria não encontrada.' USING ERRCODE = '23503';
    END IF;

    IF categoria_tipo = 'PERSONALIZADA'
       AND (categoria_contexto_id <> NEW.contexto_id OR categoria_data_exclusao IS NOT NULL) THEN
        RAISE EXCEPTION 'Categoria personalizada indisponível para o contexto da transação.'
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER transacao_categoria_contexto_trg
    BEFORE INSERT OR UPDATE OF contexto_id, categoria_id
    ON transacao
    FOR EACH ROW
    EXECUTE FUNCTION validar_categoria_da_transacao();
