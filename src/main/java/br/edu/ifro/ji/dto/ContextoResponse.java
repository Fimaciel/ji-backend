package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.ContextoParticipante;
import br.edu.ifro.ji.model.PapelParticipante;
import br.edu.ifro.ji.model.TipoContexto;

public record ContextoResponse(
        Long id,
        String nome,
        TipoContexto tipo,
        PapelParticipante papel
) {
    public static ContextoResponse de(ContextoParticipante participante) {
        return new ContextoResponse(
                participante.getContexto().getId(),
                participante.getContexto().getNome(),
                participante.getContexto().getTipo(),
                participante.getPapel()
        );
    }
}
