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

import java.time.Instant;

@Entity
@Table(name = "contexto")
public class Contexto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_criador_id", nullable = false)
    private Usuario usuarioCriador;

    @Column(nullable = false, length = 80)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoContexto tipo;

    @Column(name = "data_criacao", nullable = false)
    private Instant dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private Instant dataAtualizacao;

    protected Contexto() {
    }

    public Contexto(Usuario usuarioCriador, String nome, TipoContexto tipo, Instant agora) {
        this.usuarioCriador = usuarioCriador;
        this.nome = nome;
        this.tipo = tipo;
        this.dataCriacao = agora;
        this.dataAtualizacao = agora;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuarioCriador() {
        return usuarioCriador;
    }

    public String getNome() {
        return nome;
    }

    public TipoContexto getTipo() {
        return tipo;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }

    public Instant getDataAtualizacao() {
        return dataAtualizacao;
    }
}
