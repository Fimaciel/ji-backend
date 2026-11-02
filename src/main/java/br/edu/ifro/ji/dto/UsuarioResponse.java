package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.Usuario;

import java.time.Instant;
import java.time.LocalDate;

public record UsuarioResponse(
        Long id,
        String nome,
        String sobrenome,
        String cpf,
        LocalDate dataNascimento,
        String email,
        Instant dataCriacao
) {
    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getSobrenome(),
                usuario.getCpf(),
                usuario.getDataNascimento(),
                usuario.getEmail(),
                usuario.getDataCriacao()
        );
    }
}
