package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.TipoTransacao;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ImportacaoRegistroRequest(
        @NotNull(message = "A data é obrigatória.")
        @PastOrPresent(message = "A data não pode ser futura.")
        LocalDate data,

        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String descricao,

        @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        BigDecimal valor,

        @NotNull(message = "O tipo é obrigatório.")
        TipoTransacao tipo,

        @NotNull(message = "A categoria é obrigatória.")
        Long categoriaId
) {
}
