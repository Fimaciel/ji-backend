package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.ConfiguracaoUsuario;
import br.edu.ifro.ji.model.Tema;

import java.math.BigDecimal;

public record ConfiguracaoUsuarioResponse(
        BigDecimal salarioMensal,
        String moeda,
        boolean notificacoesAtivas,
        Tema tema
) {
    public static ConfiguracaoUsuarioResponse de(ConfiguracaoUsuario configuracao) {
        return new ConfiguracaoUsuarioResponse(
                configuracao.getSalarioMensal(),
                configuracao.getMoeda(),
                configuracao.isNotificacoesAtivas(),
                configuracao.getTema()
        );
    }
}
