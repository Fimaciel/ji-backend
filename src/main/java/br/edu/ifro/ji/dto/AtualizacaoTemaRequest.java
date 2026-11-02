package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.Tema;
import jakarta.validation.constraints.NotNull;

public record AtualizacaoTemaRequest(
        @NotNull(message = "O tema é obrigatório.")
        Tema tema
) {
}
