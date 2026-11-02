package br.edu.ifro.ji.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record AtualizacaoConfiguracaoRequest(
        @NotNull(message = "O salário mensal é obrigatório.")
        @DecimalMin(value = "0.00", message = "O salário mensal não pode ser negativo.")
        @Digits(integer = 10, fraction = 2, message = "Informe um valor com no máximo duas casas decimais.")
        BigDecimal salarioMensal,

        @NotBlank(message = "A moeda é obrigatória.")
        @Pattern(regexp = "BRL", message = "A moeda aceita nesta versão é BRL.")
        String moeda,

        boolean notificacoesAtivas
) {
}
