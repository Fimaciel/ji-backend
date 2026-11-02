ALTER TABLE configuracao_usuario
    ADD CONSTRAINT configuracao_usuario_salario_ck
        CHECK (salario_mensal >= 0),
    ADD CONSTRAINT configuracao_usuario_moeda_ck
        CHECK (moeda = 'BRL');
