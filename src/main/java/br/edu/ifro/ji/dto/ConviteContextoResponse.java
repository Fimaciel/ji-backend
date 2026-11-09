package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.ConviteContexto;
import br.edu.ifro.ji.model.PapelParticipante;
import br.edu.ifro.ji.model.StatusConvite;

import java.time.Instant;

public record ConviteContextoResponse(
        Long id,
        Long contextoId,
        String contextoNome,
        String convidadoPor,
        PapelParticipante papelProposto,
        StatusConvite status,
        Instant dataCriacao,
        Instant dataExpiracao,
        Instant dataResposta
) {
    public static ConviteContextoResponse de(ConviteContexto convite) {
        return new ConviteContextoResponse(
                convite.getId(),
                convite.getContexto().getId(),
                convite.getContexto().getNome(),
                (convite.getUsuarioRemetente().getNome() + " "
                        + convite.getUsuarioRemetente().getSobrenome()).trim(),
                convite.getPapelProposto(),
                convite.getStatus(),
                convite.getDataCriacao(),
                convite.getDataExpiracao(),
                convite.getDataResposta()
        );
    }
}
