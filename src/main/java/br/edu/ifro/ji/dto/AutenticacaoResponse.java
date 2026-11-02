package br.edu.ifro.ji.dto;

import java.time.Instant;
import java.util.List;

public record AutenticacaoResponse(
        String tokenAcesso,
        String tipoToken,
        Instant expiraEm,
        UsuarioResponse usuario,
        Long contextoAtivoId,
        boolean selecaoContextoNecessaria,
        List<ContextoResponse> contextos
) {
}
