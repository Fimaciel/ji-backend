package br.edu.ifro.ji.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RecuperacaoSenhaRequest(
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        String email
) {
    public RecuperacaoSenhaRequest {
        email = email == null ? null : email.trim();
    }
}
