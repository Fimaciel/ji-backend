package br.edu.ifro.ji.dto;

import br.edu.ifro.ji.model.Categoria;
import br.edu.ifro.ji.model.TipoCategoria;

public record CategoriaResponse(Long id, String nome, TipoCategoria tipo) {

    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNome(),
                categoria.getTipo()
        );
    }
}
