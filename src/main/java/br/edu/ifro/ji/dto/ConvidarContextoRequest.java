package br.edu.ifro.ji.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConvidarContextoRequest(
        @NotBlank(message = "Informe o e-mail do convidado.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 120, message = "O e-mail deve ter no máximo 120 caracteres.")
        String email
) {
}
