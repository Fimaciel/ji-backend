package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.FormatoImportacao;
import br.edu.ifro.ji.model.ImportacaoArquivo;
import br.edu.ifro.ji.model.StatusImportacao;

import java.time.Instant;

public record ImportacaoResponse(
        Long id,
        String nomeArquivo,
        FormatoImportacao formato,
        StatusImportacao status,
        int totalLidos,
        int totalImportados,
        int totalDuplicados,
        String executadoPor,
        Instant dataCriacao,
        Instant dataDesfeita
) {
    public static ImportacaoResponse de(ImportacaoArquivo importacao) {
        return new ImportacaoResponse(
                importacao.getId(),
                importacao.getNomeArquivo(),
                importacao.getFormato(),
                importacao.getStatus(),
                importacao.getTotalLidos(),
                importacao.getTotalImportados(),
                importacao.getTotalDuplicados(),
                importacao.getUsuario().getNome() + " " + importacao.getUsuario().getSobrenome(),
                importacao.getDataCriacao(),
                importacao.getDataDesfeita()
        );
    }
}
