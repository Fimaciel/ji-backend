package br.edu.ifro.ji.repository;

import br.edu.ifro.ji.model.Transacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransacaoRepository extends JpaRepository<Transacao, Long>,
        JpaSpecificationExecutor<Transacao> {

    Optional<Transacao> findByIdAndContextoId(Long id, Long contextoId);

    boolean existsByCategoriaId(Long categoriaId);

    @EntityGraph(attributePaths = "categoria")
    List<Transacao> findByContextoIdAndDataBetweenOrderByDataAscIdAsc(
            Long contextoId, LocalDate inicio, LocalDate fim);

    @Modifying
    @Query("delete from Transacao t where t.importacao.id = :importacaoId")
    int excluirPorImportacao(@Param("importacaoId") Long importacaoId);

    @Query("""
            select coalesce(sum(t.valor), 0) from Transacao t
            where t.contexto.id = :contextoId
              and t.tipo = br.edu.ifro.ji.model.TipoTransacao.ENTRADA
              and t.data between :inicio and :fim
            """)
    BigDecimal somarEntradas(@Param("contextoId") Long contextoId,
                             @Param("inicio") LocalDate inicio,
                             @Param("fim") LocalDate fim);

    @Query("""
            select coalesce(sum(t.valor), 0) from Transacao t
            where t.contexto.id = :contextoId
              and t.tipo = br.edu.ifro.ji.model.TipoTransacao.SAIDA
              and t.data between :inicio and :fim
            """)
    BigDecimal somarSaidas(@Param("contextoId") Long contextoId,
                           @Param("inicio") LocalDate inicio,
                           @Param("fim") LocalDate fim);

    @Query("""
            select t.data as data,
                   sum(case when t.tipo = br.edu.ifro.ji.model.TipoTransacao.ENTRADA
                            then t.valor else 0 end) as entradas,
                   sum(case when t.tipo = br.edu.ifro.ji.model.TipoTransacao.SAIDA
                            then t.valor else 0 end) as saidas
            from Transacao t
            where t.contexto.id = :contextoId
              and t.data between :inicio and :fim
            group by t.data
            order by t.data
            """)
    List<FluxoDiarioProjection> resumirFluxoDiario(
            @Param("contextoId") Long contextoId,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);

    @Query("""
            select t.categoria.id as categoriaId,
                   t.categoria.nome as categoriaNome,
                   sum(t.valor) as total
            from Transacao t
            where t.contexto.id = :contextoId
              and t.tipo = br.edu.ifro.ji.model.TipoTransacao.SAIDA
              and t.data between :inicio and :fim
            group by t.categoria.id, t.categoria.nome
            order by sum(t.valor) desc
            """)
    List<GastoCategoriaProjection> resumirSaidasPorCategoria(
            @Param("contextoId") Long contextoId,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);

    @EntityGraph(attributePaths = "categoria")
    List<Transacao> findTop5ByContextoIdAndDataBetweenOrderByDataDescIdDesc(
            Long contextoId, LocalDate inicio, LocalDate fim);

    interface FluxoDiarioProjection {
        LocalDate getData();

        BigDecimal getEntradas();

        BigDecimal getSaidas();
    }

    interface GastoCategoriaProjection {
        Long getCategoriaId();

        String getCategoriaNome();

        BigDecimal getTotal();
    }
}
