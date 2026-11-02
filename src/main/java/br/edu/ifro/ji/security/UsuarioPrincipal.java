package br.edu.ifro.ji.security;

public record UsuarioPrincipal(
        Long usuarioId,
        String email,
        Long sessaoId,
        Long contextoAtivoId
) {
}
