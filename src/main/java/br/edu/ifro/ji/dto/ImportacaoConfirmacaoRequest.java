package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.FormatoImportacao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ImportacaoConfirmacaoRequest(
        @NotBlank(message = "O nome do arquivo é obrigatório.")
        @Size(max = 255, message = "O nome do arquivo deve ter no máximo 255 caracteres.")
        String nomeArquivo,

        @NotNull(message = "O formato é obrigatório.")
        FormatoImportacao formato,

        @NotNull(message = "O total lido é obrigatório.")
        Integer totalLidos,

        @NotNull(message = "O total de duplicados é obrigatório.")
        Integer totalDuplicados,

        @NotEmpty(message = "Selecione ao menos um registro para importar.")
        @Size(max = 1000, message = "Cada importação aceita no máximo 1.000 registros.")
        List<@Valid ImportacaoRegistroRequest> registros
) {
}
