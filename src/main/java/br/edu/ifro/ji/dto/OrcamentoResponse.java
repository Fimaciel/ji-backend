package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.OrcamentoCategoria;
import br.edu.ifro.ji.model.SituacaoOrcamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record OrcamentoResponse(
        Long id,
        CategoriaResponse categoria,
        BigDecimal valorLimite,
        BigDecimal valorConsumido,
        BigDecimal percentualConsumido,
        SituacaoOrcamento situacao,
        String mesReferencia
) {

    public static OrcamentoResponse de(OrcamentoCategoria orcamento,
                                       BigDecimal valorConsumido) {
        BigDecimal consumo = valorConsumido == null ? BigDecimal.ZERO : valorConsumido;
        BigDecimal percentual = consumo
                .multiply(BigDecimal.valueOf(100))
                .divide(orcamento.getValorLimite(), 2, RoundingMode.HALF_UP);
        SituacaoOrcamento situacao = percentual.compareTo(BigDecimal.valueOf(100)) >= 0
                ? SituacaoOrcamento.EXCEDIDO
                : percentual.compareTo(BigDecimal.valueOf(80)) >= 0
                        ? SituacaoOrcamento.ATENCAO
                        : SituacaoOrcamento.NORMAL;
        return new OrcamentoResponse(
                orcamento.getId(),
                CategoriaResponse.de(orcamento.getCategoria()),
                orcamento.getValorLimite(),
                consumo,
                percentual,
                situacao,
                orcamento.getMesReferencia().toString().substring(0, 7)
        );
    }
}
