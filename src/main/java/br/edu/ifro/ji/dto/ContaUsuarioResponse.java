package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.ConfiguracaoUsuario;
import br.edu.ifro.ji.model.Usuario;

import java.time.Instant;
import java.time.LocalDate;

public record ContaUsuarioResponse(
        Long id,
        String nome,
        String sobrenome,
        String cpf,
        LocalDate dataNascimento,
        String email,
        Instant dataCriacao,
        Instant dataAtualizacao,
        ConfiguracaoUsuarioResponse configuracao
) {
    public static ContaUsuarioResponse de(Usuario usuario, ConfiguracaoUsuario configuracao) {
        return new ContaUsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getSobrenome(),
                usuario.getCpf(),
                usuario.getDataNascimento(),
                usuario.getEmail(),
                usuario.getDataCriacao(),
                usuario.getDataAtualizacao(),
                ConfiguracaoUsuarioResponse.de(configuracao)
        );
    }
}
