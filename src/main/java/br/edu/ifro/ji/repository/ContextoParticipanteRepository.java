package br.edu.ifro.ji.repository;

import br.edu.ifro.ji.model.ContextoParticipante;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContextoParticipanteRepository extends JpaRepository<ContextoParticipante, Long> {

    @EntityGraph(attributePaths = "contexto")
    List<ContextoParticipante> findByUsuarioIdAndDataSaidaIsNullOrderByDataIngressoAsc(Long usuarioId);

    boolean existsByContextoIdAndUsuarioIdAndDataSaidaIsNull(Long contextoId, Long usuarioId);
}
