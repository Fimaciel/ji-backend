package br.edu.ifro.ji.dto;

import java.time.Instant;
import java.util.List;

public record SessaoResponse(
        UsuarioResponse usuario,
        Long contextoAtivoId,
        Instant expiraEm,
        boolean selecaoContextoNecessaria,
        List<ContextoResponse> contextos
) {
}
