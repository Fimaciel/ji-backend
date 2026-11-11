package br.edu.ifro.ji.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record RelatorioFluxoResponse(
        YearMonth mes,
        BigDecimal entradas,
        BigDecimal saidas
) {
}
