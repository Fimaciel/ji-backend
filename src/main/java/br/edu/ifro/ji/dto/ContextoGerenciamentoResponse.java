package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.ContextoParticipante;
import br.edu.ifro.ji.model.PapelParticipante;
import br.edu.ifro.ji.model.TipoContexto;

public record ContextoGerenciamentoResponse(
        Long id,
        String nome,
        TipoContexto tipo,
        PapelParticipante papel,
        long quantidadeParticipantes,
        boolean ativo
) {
    public static ContextoGerenciamentoResponse de(ContextoParticipante participante,
                                                   long quantidadeParticipantes,
                                                   Long contextoAtivoId) {
        return new ContextoGerenciamentoResponse(
                participante.getContexto().getId(),
                participante.getContexto().getNome(),
                participante.getContexto().getTipo(),
                participante.getPapel(),
                quantidadeParticipantes,
                participante.getContexto().getId().equals(contextoAtivoId)
        );
    }
}
