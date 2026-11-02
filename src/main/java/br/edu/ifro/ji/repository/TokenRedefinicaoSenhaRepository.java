package br.edu.ifro.ji.repository;

import br.edu.ifro.ji.model.TokenRedefinicaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TokenRedefinicaoSenhaRepository extends JpaRepository<TokenRedefinicaoSenha, Long> {

    Optional<TokenRedefinicaoSenha> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TokenRedefinicaoSenha t set t.utilizado = true " +
            "where t.usuario.id = :usuarioId and t.utilizado = false")
    int invalidarTokensPendentes(Long usuarioId);
}
