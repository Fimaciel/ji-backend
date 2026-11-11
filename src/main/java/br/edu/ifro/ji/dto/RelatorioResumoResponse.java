package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RelatorioResumoResponse(
        LocalDate dataInicial,
        LocalDate dataFinal,
        TipoTransacao tipo,
        List<String> contextos,
        BigDecimal totalEntradas,
        BigDecimal totalSaidas,
        BigDecimal saldo,
        int quantidadeTransacoes,
        List<RelatorioFluxoResponse> fluxoMensal,
        List<RelatorioCategoriaResponse> categorias,
        List<RelatorioTransacaoResponse> transacoes
) {
}
