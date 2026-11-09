package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.ContextoParticipante;
import br.edu.ifro.ji.model.PapelParticipante;

import java.time.Instant;

public record ParticipanteContextoResponse(
        Long id,
        Long usuarioId,
        String nome,
        String email,
        PapelParticipante papel,
        Instant dataIngresso,
        Instant dataSaida,
        boolean ativo
) {
    public static ParticipanteContextoResponse de(ContextoParticipante participante) {
        return new ParticipanteContextoResponse(
                participante.getId(),
                participante.getUsuario().getId(),
                (participante.getUsuario().getNome() + " "
                        + participante.getUsuario().getSobrenome()).trim(),
                participante.getUsuario().getEmail(),
                participante.getPapel(),
                participante.getDataIngresso(),
                participante.getDataSaida(),
                participante.getDataSaida() == null
        );
    }
}
