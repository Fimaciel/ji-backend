package br.edu.ifro.ji.repository;

import br.edu.ifro.ji.model.ConviteContexto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConviteContextoRepository extends JpaRepository<ConviteContexto, Long> {

    @EntityGraph(attributePaths = {"contexto", "usuarioRemetente"})
    List<ConviteContexto> findByEmailConvidadoIgnoreCaseOrderByDataCriacaoDesc(String email);

    @EntityGraph(attributePaths = {"contexto", "usuarioRemetente"})
    Optional<ConviteContexto> findDetalhadoById(Long id);

    @EntityGraph(attributePaths = {"contexto", "usuarioRemetente"})
    Optional<ConviteContexto> findByTokenHash(String tokenHash);

    @EntityGraph(attributePaths = {"contexto", "usuarioRemetente", "dependente"})
    List<ConviteContexto> findByContextoIdOrderByDataCriacaoDesc(Long contextoId);

    @EntityGraph(attributePaths = {"contexto", "usuarioRemetente", "dependente"})
    Optional<ConviteContexto> findByIdAndContextoId(Long id, Long contextoId);

    Optional<ConviteContexto> findByContextoIdAndEmailConvidadoIgnoreCaseAndStatus(
            Long contextoId, String email, br.edu.ifro.ji.model.StatusConvite status);

    List<ConviteContexto> findByDependenteIdAndStatus(Long dependenteId,
                                                      br.edu.ifro.ji.model.StatusConvite status);
}
