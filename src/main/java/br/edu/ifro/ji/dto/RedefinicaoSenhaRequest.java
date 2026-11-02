package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.validation.Senha;
import jakarta.validation.constraints.NotBlank;

public record RedefinicaoSenhaRequest(
        @NotBlank(message = "O token é obrigatório.")
        String token,

        @NotBlank(message = "A nova senha é obrigatória.")
        @Senha
        String novaSenha,

        @NotBlank(message = "A confirmação da senha é obrigatória.")
        String confirmacaoSenha
) {
}
