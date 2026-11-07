package br.edu.ifro.ji.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "orcamento_categoria",
        uniqueConstraints = @UniqueConstraint(
                name = "orcamento_contexto_categoria_mes_uk",
                columnNames = {"contexto_id", "categoria_id", "mes_referencia"}
        )
)
public class OrcamentoCategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "contexto_id", nullable = false)
    private Contexto contexto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "valor_limite", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorLimite;

    @Column(name = "mes_referencia", nullable = false)
    private LocalDate mesReferencia;

    @Column(name = "data_criacao", nullable = false)
    private Instant dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private Instant dataAtualizacao;

    protected OrcamentoCategoria() {
    }

    public OrcamentoCategoria(Contexto contexto, Categoria categoria,
                              BigDecimal valorLimite, LocalDate mesReferencia,
                              Instant agora) {
        this.contexto = contexto;
        this.categoria = categoria;
        this.valorLimite = valorLimite;
        this.mesReferencia = mesReferencia;
        this.dataCriacao = agora;
        this.dataAtualizacao = agora;
    }

    public void atualizar(Categoria categoria, BigDecimal valorLimite,
                          LocalDate mesReferencia, Instant agora) {
        this.categoria = categoria;
        this.valorLimite = valorLimite;
        this.mesReferencia = mesReferencia;
        this.dataAtualizacao = agora;
    }

    public Long getId() {
        return id;
    }

    public Contexto getContexto() {
        return contexto;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public BigDecimal getValorLimite() {
        return valorLimite;
    }

    public LocalDate getMesReferencia() {
        return mesReferencia;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }

    public Instant getDataAtualizacao() {
        return dataAtualizacao;
    }
}
