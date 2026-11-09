package br.edu.ifro.ji.dto;

import jakarta.validation.constraints.NotNull;

public record SelecionarContextoRequest(
        @NotNull(message = "Informe o contexto que deseja ativar.")
        Long contextoId
) {
}
