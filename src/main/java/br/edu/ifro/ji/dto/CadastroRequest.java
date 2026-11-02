package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.validation.Senha;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CadastroRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 60, message = "O nome deve ter no máximo 60 caracteres.")
        String nome,

        @NotBlank(message = "O sobrenome é obrigatório.")
        @Size(max = 80, message = "O sobrenome deve ter no máximo 80 caracteres.")
        String sobrenome,

        @Pattern(regexp = "^$|\\d{11}", message = "O CPF deve conter 11 dígitos.")
        String cpf,

        @PastOrPresent(message = "A data de nascimento não pode estar no futuro.")
        LocalDate dataNascimento,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 120, message = "O e-mail deve ter no máximo 120 caracteres.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        @Senha
        String senha
) {
    public CadastroRequest {
        nome = nome == null ? null : nome.trim();
        sobrenome = sobrenome == null ? null : sobrenome.trim();
        cpf = cpf == null || cpf.isBlank() ? null : cpf.trim();
        email = email == null ? null : email.trim();
    }
}
