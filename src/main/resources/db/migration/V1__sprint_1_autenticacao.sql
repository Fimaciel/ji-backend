CREATE TABLE usuario (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(60) NOT NULL,
    sobrenome VARCHAR(80) NOT NULL,
    cpf VARCHAR(11),
    data_nascimento DATE,
    email VARCHAR(120) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    data_criacao TIMESTAMPTZ NOT NULL,
    data_atualizacao TIMESTAMPTZ NOT NULL,
    data_exclusao TIMESTAMPTZ,
    CONSTRAINT usuario_cpf_formato_ck CHECK (cpf IS NULL OR cpf ~ '^[0-9]{11}$'),
    CONSTRAINT usuario_cpf_uk UNIQUE (cpf)
);

CREATE UNIQUE INDEX usuario_email_ativo_uk
    ON usuario (LOWER(email))
    WHERE data_exclusao IS NULL;

CREATE TABLE configuracao_usuario (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    salario_mensal NUMERIC(12, 2) NOT NULL DEFAULT 0,
    moeda VARCHAR(3) NOT NULL DEFAULT 'BRL',
    notificacoes_ativas BOOLEAN NOT NULL DEFAULT TRUE,
    tema VARCHAR(12) NOT NULL DEFAULT 'AUTOMATICO',
    CONSTRAINT configuracao_usuario_usuario_uk UNIQUE (usuario_id),
    CONSTRAINT configuracao_usuario_tema_ck
        CHECK (tema IN ('CLARO', 'ESCURO', 'AUTOMATICO'))
);

CREATE TABLE contexto (
    id BIGSERIAL PRIMARY KEY,
    usuario_criador_id BIGINT NOT NULL REFERENCES usuario(id),
    nome VARCHAR(80) NOT NULL,
    tipo VARCHAR(15) NOT NULL,
    data_criacao TIMESTAMPTZ NOT NULL,
    data_atualizacao TIMESTAMPTZ NOT NULL,
    CONSTRAINT contexto_tipo_ck CHECK (tipo IN ('PESSOAL', 'COMPARTILHADO'))
);

CREATE UNIQUE INDEX contexto_pessoal_usuario_uk
    ON contexto (usuario_criador_id)
    WHERE tipo = 'PESSOAL';

CREATE TABLE contexto_participante (
    id BIGSERIAL PRIMARY KEY,
    contexto_id BIGINT NOT NULL REFERENCES contexto(id),
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    papel VARCHAR(25) NOT NULL,
    data_ingresso TIMESTAMPTZ NOT NULL,
    data_saida TIMESTAMPTZ,
    CONSTRAINT contexto_participante_papel_ck
        CHECK (papel IN ('ADMINISTRADOR', 'COLABORADOR', 'DEPENDENTE'))
);

CREATE UNIQUE INDEX contexto_participante_ativo_uk
    ON contexto_participante (contexto_id, usuario_id)
    WHERE data_saida IS NULL;

CREATE INDEX contexto_participante_usuario_idx
    ON contexto_participante (usuario_id)
    WHERE data_saida IS NULL;

CREATE TABLE token_redefinicao_senha (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    token VARCHAR(64) NOT NULL,
    data_criacao TIMESTAMPTZ NOT NULL,
    data_expiracao TIMESTAMPTZ NOT NULL,
    utilizado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT token_redefinicao_senha_token_uk UNIQUE (token)
);

CREATE INDEX token_redefinicao_senha_usuario_idx
    ON token_redefinicao_senha (usuario_id, utilizado);

-- Tabela técnica da autenticação. O JWT é validado criptograficamente e o
-- identificador jti liga o token à sessão revogável no servidor.
CREATE TABLE sessao (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    contexto_ativo_id BIGINT REFERENCES contexto(id),
    jti UUID NOT NULL,
    data_criacao TIMESTAMPTZ NOT NULL,
    data_expiracao TIMESTAMPTZ NOT NULL,
    data_revogacao TIMESTAMPTZ,
    CONSTRAINT sessao_jti_uk UNIQUE (jti)
);

CREATE INDEX sessao_usuario_ativa_idx
    ON sessao (usuario_id)
    WHERE data_revogacao IS NULL;

CREATE TABLE log_auditoria (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    contexto_id BIGINT REFERENCES contexto(id),
    acao VARCHAR(40) NOT NULL,
    entidade_afetada VARCHAR(40) NOT NULL,
    registro_id BIGINT NOT NULL,
    data_hora TIMESTAMPTZ NOT NULL,
    detalhes VARCHAR(255) NOT NULL
);

CREATE INDEX log_auditoria_usuario_data_idx
    ON log_auditoria (usuario_id, data_hora DESC);
