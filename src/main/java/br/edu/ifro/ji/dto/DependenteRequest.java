package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.TipoDependente;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DependenteRequest(
        @NotBlank(message = "Informe o nome do dependente.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String nome,

        @NotNull(message = "Informe a data de nascimento.")
        @PastOrPresent(message = "A data de nascimento não pode estar no futuro.")
        LocalDate dataNascimento,

        @NotNull(message = "Informe o tipo do dependente.")
        TipoDependente tipo,

        @Email(message = "Informe um e-mail válido.")
        @Size(max = 120, message = "O e-mail deve ter no máximo 120 caracteres.")
        String email
) {
}
