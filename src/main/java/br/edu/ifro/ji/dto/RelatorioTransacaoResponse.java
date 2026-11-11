package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RelatorioTransacaoResponse(
        Long id,
        String contexto,
        LocalDate data,
        String descricao,
        String categoria,
        TipoTransacao tipo,
        BigDecimal valor
) {
}
