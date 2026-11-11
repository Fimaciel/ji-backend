package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ImportacaoRegistroResponse(
        int linha,
        LocalDate data,
        String descricao,
        BigDecimal valor,
        TipoTransacao tipo,
        CategoriaResponse categoriaSugerida,
        boolean duplicado
) {
}
