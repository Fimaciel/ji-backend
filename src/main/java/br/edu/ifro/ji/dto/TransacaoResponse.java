package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.TipoTransacao;
import br.edu.ifro.ji.model.Transacao;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TransacaoResponse(
        Long id,
        TipoTransacao tipo,
        BigDecimal valor,
        LocalDate data,
        String descricao,
        CategoriaResponse categoria,
        Instant dataCriacao,
        Instant dataAtualizacao
) {

    public static TransacaoResponse de(Transacao transacao) {
        return new TransacaoResponse(
                transacao.getId(),
                transacao.getTipo(),
                transacao.getValor(),
                transacao.getData(),
                transacao.getDescricao(),
                CategoriaResponse.de(transacao.getCategoria()),
                transacao.getDataCriacao(),
                transacao.getDataAtualizacao()
        );
    }
}
