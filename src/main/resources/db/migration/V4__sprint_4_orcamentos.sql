CREATE TABLE orcamento_categoria (
    id BIGSERIAL PRIMARY KEY,
    contexto_id BIGINT NOT NULL REFERENCES contexto(id),
    categoria_id BIGINT NOT NULL REFERENCES categoria(id),
    valor_limite NUMERIC(12, 2) NOT NULL,
    mes_referencia DATE NOT NULL,
    data_criacao TIMESTAMPTZ NOT NULL,
    data_atualizacao TIMESTAMPTZ NOT NULL,
    CONSTRAINT orcamento_valor_limite_ck CHECK (valor_limite > 0),
    CONSTRAINT orcamento_mes_referencia_ck CHECK (
        mes_referencia = DATE_TRUNC('month', mes_referencia)::DATE
    ),
    CONSTRAINT orcamento_contexto_categoria_mes_uk UNIQUE (
        contexto_id, categoria_id, mes_referencia
    )
);

CREATE INDEX orcamento_contexto_mes_idx
    ON orcamento_categoria (contexto_id, mes_referencia);

CREATE OR REPLACE FUNCTION validar_categoria_do_orcamento()
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
        RAISE EXCEPTION 'Categoria personalizada indisponível para o contexto do orçamento.'
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER orcamento_categoria_contexto_trg
    BEFORE INSERT OR UPDATE OF contexto_id, categoria_id
    ON orcamento_categoria
    FOR EACH ROW
    EXECUTE FUNCTION validar_categoria_do_orcamento();
