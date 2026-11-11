package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.FormatoImportacao;

import java.util.List;

public record ImportacaoAnaliseResponse(
        String nomeArquivo,
        FormatoImportacao formato,
        int totalLidos,
        int totalNovos,
        int totalDuplicados,
        List<ImportacaoRegistroResponse> registros
) {
}
