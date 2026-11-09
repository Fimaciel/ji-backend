package br.edu.ifro.ji.repository;

import br.edu.ifro.ji.model.Dependente;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DependenteRepository extends JpaRepository<Dependente, Long> {

    @EntityGraph(attributePaths = {"participante", "participante.usuario"})
    List<Dependente> findByContextoIdAndDataExclusaoIsNullOrderByNomeAsc(Long contextoId);

    @EntityGraph(attributePaths = {"contexto", "participante", "participante.usuario"})
    Optional<Dependente> findByIdAndContextoIdAndDataExclusaoIsNull(Long id, Long contextoId);

    Optional<Dependente> findByParticipanteIdAndDataExclusaoIsNull(Long participanteId);
}
