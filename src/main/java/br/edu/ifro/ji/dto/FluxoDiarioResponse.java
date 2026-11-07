package br.edu.ifro.ji.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FluxoDiarioResponse(
        LocalDate data,
        BigDecimal entradas,
        BigDecimal saidas,
        BigDecimal saldoAcumulado
) {
}
