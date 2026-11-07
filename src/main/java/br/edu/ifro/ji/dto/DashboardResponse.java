package br.edu.ifro.ji.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DashboardResponse(
        String mesReferencia,
        BigDecimal totalEntradas,
        BigDecimal totalSaidas,
        BigDecimal saldo,
        List<FluxoDiarioResponse> fluxoDiario,
        List<CategoriaDashboardResponse> principaisCategorias,
        List<TransacaoResponse> transacoesRecentes,
        Instant geradoEm
) {
}
