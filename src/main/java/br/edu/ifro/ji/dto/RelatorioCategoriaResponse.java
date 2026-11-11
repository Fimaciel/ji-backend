package br.edu.ifro.ji.dto;

import java.math.BigDecimal;

public record RelatorioCategoriaResponse(
        String categoria,
        long quantidade,
        BigDecimal total,
        BigDecimal media
) {
}
