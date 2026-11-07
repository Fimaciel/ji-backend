package br.edu.ifro.ji.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record OrcamentoRequest(
        @NotNull(message = "Informe a categoria.")
        Long categoriaId,

        @NotNull(message = "Informe o valor-limite mensal.")
        @DecimalMin(value = "0.01", message = "O valor-limite deve ser maior que zero.")
        @Digits(integer = 10, fraction = 2,
                message = "O valor-limite deve ter no máximo duas casas decimais.")
        BigDecimal valorLimite,

        @NotBlank(message = "Informe o mês de referência.")
        @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])",
                message = "Informe o mês no formato AAAA-MM.")
        String mesReferencia
) {
}
