package br.edu.ifro.ji.repository;

import br.edu.ifro.ji.model.Sessao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SessaoRepository extends JpaRepository<Sessao, Long> {

    @EntityGraph(attributePaths = {"usuario", "contextoAtivo"})
    Optional<Sessao> findByJti(UUID jti);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Sessao s set s.dataRevogacao = :agora " +
            "where s.usuario.id = :usuarioId and s.dataRevogacao is null")
    int revogarTodasDoUsuario(Long usuarioId, Instant agora);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Sessao s set s.contextoAtivo = null " +
            "where s.usuario.id = :usuarioId and s.contextoAtivo.id = :contextoId " +
            "and s.dataRevogacao is null")
    int limparContextoAtivo(Long usuarioId, Long contextoId);
}
