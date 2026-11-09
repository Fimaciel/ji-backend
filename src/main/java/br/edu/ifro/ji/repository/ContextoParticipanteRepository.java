package br.edu.ifro.ji.repository;

import br.edu.ifro.ji.model.ContextoParticipante;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContextoParticipanteRepository extends JpaRepository<ContextoParticipante, Long> {

    @EntityGraph(attributePaths = "contexto")
    List<ContextoParticipante> findByUsuarioIdAndDataSaidaIsNullOrderByDataIngressoAsc(Long usuarioId);

    boolean existsByContextoIdAndUsuarioIdAndDataSaidaIsNull(Long contextoId, Long usuarioId);

    long countByContextoIdAndDataSaidaIsNull(Long contextoId);

    long countByContextoIdAndPapelAndDataSaidaIsNull(Long contextoId,
                                                     br.edu.ifro.ji.model.PapelParticipante papel);

    @EntityGraph(attributePaths = {"contexto", "usuario"})
    List<ContextoParticipante> findByContextoIdOrderByDataIngressoAsc(Long contextoId);

    @EntityGraph(attributePaths = {"contexto", "usuario"})
    Optional<ContextoParticipante> findByIdAndContextoId(Long id, Long contextoId);

    @EntityGraph(attributePaths = {"contexto", "usuario"})
    Optional<ContextoParticipante> findByContextoIdAndUsuarioIdAndDataSaidaIsNull(
            Long contextoId, Long usuarioId);
}
