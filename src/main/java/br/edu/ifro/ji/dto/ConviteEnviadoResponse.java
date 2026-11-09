package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.ConviteContexto;
import br.edu.ifro.ji.model.PapelParticipante;
import br.edu.ifro.ji.model.StatusConvite;

import java.time.Instant;

public record ConviteEnviadoResponse(
        Long id,
        String emailConvidado,
        PapelParticipante papelProposto,
        StatusConvite status,
        Long dependenteId,
        Instant dataCriacao,
        Instant dataExpiracao,
        Instant dataResposta
) {
    public static ConviteEnviadoResponse de(ConviteContexto convite) {
        return new ConviteEnviadoResponse(
                convite.getId(),
                convite.getEmailConvidado(),
                convite.getPapelProposto(),
                convite.getStatus(),
                convite.getDependente() == null ? null : convite.getDependente().getId(),
                convite.getDataCriacao(),
                convite.getDataExpiracao(),
                convite.getDataResposta()
        );
    }
}
