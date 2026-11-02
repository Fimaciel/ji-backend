package br.edu.ifro.ji.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        String senha
) {
    public LoginRequest {
        email = email == null ? null : email.trim();
    }
}
