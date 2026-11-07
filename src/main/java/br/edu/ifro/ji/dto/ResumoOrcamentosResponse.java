package br.edu.ifro.ji.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record ResumoOrcamentosResponse(
        String mesReferencia,
        BigDecimal totalLimite,
        BigDecimal totalConsumido,
        BigDecimal percentualConsumido,
        List<OrcamentoResponse> itens
) {

    public static ResumoOrcamentosResponse de(String mesReferencia,
                                              List<OrcamentoResponse> itens) {
        BigDecimal limite = itens.stream()
                .map(OrcamentoResponse::valorLimite)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal consumo = itens.stream()
                .map(OrcamentoResponse::valorConsumido)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal percentual = limite.signum() == 0
                ? BigDecimal.ZERO
                : consumo.multiply(BigDecimal.valueOf(100))
                        .divide(limite, 2, RoundingMode.HALF_UP);
        return new ResumoOrcamentosResponse(
                mesReferencia, limite, consumo, percentual, itens);
    }
}
