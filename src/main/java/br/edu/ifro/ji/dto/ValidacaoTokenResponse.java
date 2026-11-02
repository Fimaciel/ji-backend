package br.edu.ifro.ji.dto;

import java.time.Instant;

public record ValidacaoTokenResponse(
        String status,
        String email,
        Instant expiraEm
) {
}
