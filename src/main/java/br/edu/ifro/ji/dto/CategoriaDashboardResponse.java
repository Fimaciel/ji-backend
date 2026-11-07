package br.edu.ifro.ji.dto;

import java.math.BigDecimal;

public record CategoriaDashboardResponse(
        Long categoriaId,
        String categoriaNome,
        BigDecimal total,
        BigDecimal percentual
) {
}
