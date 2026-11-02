package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.validation.Senha;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AtualizacaoPerfilRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 60, message = "O nome deve ter no máximo 60 caracteres.")
        String nome,

        @NotBlank(message = "O sobrenome é obrigatório.")
        @Size(max = 80, message = "O sobrenome deve ter no máximo 80 caracteres.")
        String sobrenome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 120, message = "O e-mail deve ter no máximo 120 caracteres.")
        String email,

        @PastOrPresent(message = "A data de nascimento não pode estar no futuro.")
        LocalDate dataNascimento,

        @NotBlank(message = "A senha atual é obrigatória.")
        String senhaAtual,

        @Senha
        String novaSenha,

        String confirmacaoNovaSenha
) {
    public AtualizacaoPerfilRequest {
        nome = nome == null ? null : nome.trim();
        sobrenome = sobrenome == null ? null : sobrenome.trim();
        email = email == null ? null : email.trim();
        novaSenha = novaSenha == null || novaSenha.isBlank() ? null : novaSenha;
        confirmacaoNovaSenha = confirmacaoNovaSenha == null || confirmacaoNovaSenha.isBlank()
                ? null
                : confirmacaoNovaSenha;
    }
}
