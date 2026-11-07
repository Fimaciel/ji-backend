package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.TipoTransacao;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransacaoRequest(
        @NotNull(message = "Informe o tipo da transação.")
        TipoTransacao tipo,

        @NotNull(message = "Informe o valor.")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        @Digits(integer = 10, fraction = 2,
                message = "O valor deve ter no máximo duas casas decimais.")
        BigDecimal valor,

        @NotNull(message = "Informe a data.")
        @PastOrPresent(message = "A data não pode ser futura.")
        LocalDate data,

        @NotNull(message = "Informe a categoria.")
        Long categoriaId,

        @NotBlank(message = "Informe a descrição.")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String descricao
) {
}
