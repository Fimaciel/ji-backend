package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.PapelParticipante;
import jakarta.validation.constraints.NotNull;

public record AlterarPapelRequest(
        @NotNull(message = "Informe o novo papel.")
        PapelParticipante papel
) {
}
