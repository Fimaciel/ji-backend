package br.edu.ifro.ji.dto;

import java.time.Instant;

public record SessaoResponse(
        UsuarioResponse usuario,
        Long contextoAtivoId,
        Instant expiraEm
) {
}
