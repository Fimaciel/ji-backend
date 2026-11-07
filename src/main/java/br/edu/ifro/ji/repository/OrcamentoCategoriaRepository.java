package br.edu.ifro.ji.repository;

import br.edu.ifro.ji.model.OrcamentoCategoria;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrcamentoCategoriaRepository
        extends JpaRepository<OrcamentoCategoria, Long> {

    @EntityGraph(attributePaths = "categoria")
    List<OrcamentoCategoria> findByContextoIdAndMesReferenciaOrderByCategoriaNomeAsc(
            Long contextoId, LocalDate mesReferencia);

    @EntityGraph(attributePaths = "categoria")
    Optional<OrcamentoCategoria> findByIdAndContextoId(Long id, Long contextoId);

    boolean existsByContextoIdAndCategoriaIdAndMesReferencia(
            Long contextoId, Long categoriaId, LocalDate mesReferencia);

    boolean existsByContextoIdAndCategoriaIdAndMesReferenciaAndIdNot(
            Long contextoId, Long categoriaId, LocalDate mesReferencia, Long id);

    boolean existsByCategoriaId(Long categoriaId);
}
