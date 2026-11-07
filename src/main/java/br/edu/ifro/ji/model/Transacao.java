package br.edu.ifro.ji.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "transacao")
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "contexto_id", nullable = false)
    private Contexto contexto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoTransacao tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Column(name = "data_criacao", nullable = false)
    private Instant dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private Instant dataAtualizacao;

    protected Transacao() {
    }

    public Transacao(Contexto contexto, Usuario usuario, Categoria categoria,
                     TipoTransacao tipo, BigDecimal valor, LocalDate data,
                     String descricao, Instant agora) {
        this.contexto = contexto;
        this.usuario = usuario;
        this.categoria = categoria;
        this.tipo = tipo;
        this.valor = valor;
        this.data = data;
        this.descricao = descricao;
        this.dataCriacao = agora;
        this.dataAtualizacao = agora;
    }

    public void atualizar(Categoria categoria, TipoTransacao tipo, BigDecimal valor,
                          LocalDate data, String descricao, Instant agora) {
        this.categoria = categoria;
        this.tipo = tipo;
        this.valor = valor;
        this.data = data;
        this.descricao = descricao;
        this.dataAtualizacao = agora;
    }

    public Long getId() {
        return id;
    }

    public Contexto getContexto() {
        return contexto;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getData() {
        return data;
    }

    public String getDescricao() {
        return descricao;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }

    public Instant getDataAtualizacao() {
        return dataAtualizacao;
    }
}
