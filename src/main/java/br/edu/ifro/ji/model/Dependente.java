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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "dependente")
public class Dependente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "contexto_id", nullable = false)
    private Contexto contexto;

    @OneToOne
    @JoinColumn(name = "participante_id", unique = true)
    private ContextoParticipante participante;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoDependente tipo;

    @Column(name = "data_criacao", nullable = false)
    private Instant dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private Instant dataAtualizacao;

    @Column(name = "data_exclusao")
    private Instant dataExclusao;

    protected Dependente() {
    }

    public Dependente(Contexto contexto, String nome, LocalDate dataNascimento,
                      TipoDependente tipo, Instant agora) {
        this.contexto = contexto;
        this.nome = nome;
        this.dataNascimento = dataNascimento;
        this.tipo = tipo;
        this.dataCriacao = agora;
        this.dataAtualizacao = agora;
    }

    public void atualizar(String nome, LocalDate dataNascimento, TipoDependente tipo, Instant agora) {
        this.nome = nome;
        this.dataNascimento = dataNascimento;
        this.tipo = tipo;
        this.dataAtualizacao = agora;
    }

    public void vincularParticipante(ContextoParticipante participante, Instant agora) {
        this.participante = participante;
        this.dataAtualizacao = agora;
    }

    public void desvincularParticipante(Instant agora) {
        this.participante = null;
        this.dataAtualizacao = agora;
    }

    public void excluir(Instant agora) {
        this.dataExclusao = agora;
        this.dataAtualizacao = agora;
    }

    public Long getId() { return id; }
    public Contexto getContexto() { return contexto; }
    public ContextoParticipante getParticipante() { return participante; }
    public String getNome() { return nome; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public TipoDependente getTipo() { return tipo; }
    public Instant getDataCriacao() { return dataCriacao; }
    public Instant getDataAtualizacao() { return dataAtualizacao; }
    public Instant getDataExclusao() { return dataExclusao; }
}
