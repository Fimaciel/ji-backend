package br.edu.ifro.ji.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank(message = "Informe o nome da categoria.")
        @Size(max = 60, message = "O nome deve ter no máximo 60 caracteres.")
        String nome
) {
}
