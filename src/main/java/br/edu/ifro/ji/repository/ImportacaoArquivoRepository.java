package br.edu.ifro.ji.repository;

import br.edu.ifro.ji.model.ImportacaoArquivo;
import br.edu.ifro.ji.model.StatusImportacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImportacaoArquivoRepository extends JpaRepository<ImportacaoArquivo, Long> {

    @EntityGraph(attributePaths = "usuario")
    List<ImportacaoArquivo> findTop20ByContextoIdOrderByDataCriacaoDescIdDesc(Long contextoId);

    @EntityGraph(attributePaths = {"contexto", "usuario"})
    Optional<ImportacaoArquivo> findFirstByContextoIdAndStatusOrderByDataCriacaoDescIdDesc(
            Long contextoId, StatusImportacao status);
}
