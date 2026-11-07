package br.edu.ifro.ji.repository;

import br.edu.ifro.ji.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    @Query("""
            select c from Categoria c
            where c.tipo = br.edu.ifro.ji.model.TipoCategoria.PADRAO
               or (c.contexto.id = :contextoId and c.dataExclusao is null)
            order by c.tipo, lower(c.nome)
            """)
    List<Categoria> listarDisponiveis(@Param("contextoId") Long contextoId);

    @Query("""
            select c from Categoria c
            where c.id = :categoriaId
              and c.dataExclusao is null
              and (c.tipo = br.edu.ifro.ji.model.TipoCategoria.PADRAO
                   or c.contexto.id = :contextoId)
            """)
    Optional<Categoria> buscarDisponivel(@Param("categoriaId") Long categoriaId,
                                         @Param("contextoId") Long contextoId);

    @Query("""
            select c from Categoria c
            where c.id = :categoriaId
              and c.contexto.id = :contextoId
              and c.tipo = br.edu.ifro.ji.model.TipoCategoria.PERSONALIZADA
              and c.dataExclusao is null
            """)
    Optional<Categoria> buscarPersonalizadaAtiva(@Param("categoriaId") Long categoriaId,
                                                  @Param("contextoId") Long contextoId);

    @Query("""
            select (count(c) > 0) from Categoria c
            where lower(c.nome) = lower(:nome)
              and c.dataExclusao is null
              and (c.tipo = br.edu.ifro.ji.model.TipoCategoria.PADRAO
                   or c.contexto.id = :contextoId)
              and (:categoriaIgnoradaId is null or c.id <> :categoriaIgnoradaId)
            """)
    boolean existeDisponivelComNome(@Param("contextoId") Long contextoId,
                                    @Param("nome") String nome,
                                    @Param("categoriaIgnoradaId") Long categoriaIgnoradaId);
}
