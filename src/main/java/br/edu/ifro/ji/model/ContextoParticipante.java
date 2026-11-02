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
@Table(name = "contexto_participante")
public class ContextoParticipante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "contexto_id", nullable = false)
    private Contexto contexto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private PapelParticipante papel;

    @Column(name = "data_ingresso", nullable = false)
    private Instant dataIngresso;

    @Column(name = "data_saida")
    private Instant dataSaida;

    protected ContextoParticipante() {
    }

    public ContextoParticipante(Contexto contexto, Usuario usuario,
                                PapelParticipante papel, Instant agora) {
        this.contexto = contexto;
        this.usuario = usuario;
        this.papel = papel;
        this.dataIngresso = agora;
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

    public PapelParticipante getPapel() {
        return papel;
    }

    public Instant getDataIngresso() {
        return dataIngresso;
    }

    public Instant getDataSaida() {
        return dataSaida;
    }
}
