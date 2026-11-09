package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.Dependente;
import br.edu.ifro.ji.model.TipoDependente;

import java.time.Instant;
import java.time.LocalDate;

public record DependenteResponse(
        Long id,
        String nome,
        LocalDate dataNascimento,
        TipoDependente tipo,
        Long participanteId,
        String usuarioVinculado,
        Instant dataCriacao,
        Instant dataAtualizacao
) {
    public static DependenteResponse de(Dependente dependente) {
        return new DependenteResponse(
                dependente.getId(),
                dependente.getNome(),
                dependente.getDataNascimento(),
                dependente.getTipo(),
                dependente.getParticipante() == null ? null : dependente.getParticipante().getId(),
                dependente.getParticipante() == null ? null
                        : dependente.getParticipante().getUsuario().getEmail(),
                dependente.getDataCriacao(),
                dependente.getDataAtualizacao()
        );
    }
}
