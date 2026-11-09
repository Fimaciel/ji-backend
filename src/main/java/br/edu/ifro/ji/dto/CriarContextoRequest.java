package br.edu.ifro.ji.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarContextoRequest(
        @NotBlank(message = "Informe o nome do contexto.")
        @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres.")
        String nome
) {
}
