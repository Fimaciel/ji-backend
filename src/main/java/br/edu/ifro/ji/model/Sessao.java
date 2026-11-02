package br.edu.ifro.ji.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessao")
public class Sessao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "contexto_ativo_id")
    private Contexto contextoAtivo;

    @Column(nullable = false, unique = true)
    private UUID jti;

    @Column(name = "data_criacao", nullable = false)
    private Instant dataCriacao;

    @Column(name = "data_expiracao", nullable = false)
    private Instant dataExpiracao;

    @Column(name = "data_revogacao")
    private Instant dataRevogacao;

    protected Sessao() {
    }

    public Sessao(Usuario usuario, Contexto contextoAtivo, UUID jti,
                  Instant dataCriacao, Instant dataExpiracao) {
        this.usuario = usuario;
        this.contextoAtivo = contextoAtivo;
        this.jti = jti;
        this.dataCriacao = dataCriacao;
        this.dataExpiracao = dataExpiracao;
    }

    public boolean estaAtivaEm(Instant instante) {
        return dataRevogacao == null && dataExpiracao.isAfter(instante) && usuario.estaAtivo();
    }

    public void revogar(Instant instante) {
        if (dataRevogacao == null) {
            dataRevogacao = instante;
        }
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Contexto getContextoAtivo() {
        return contextoAtivo;
    }

    public UUID getJti() {
        return jti;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }

    public Instant getDataExpiracao() {
        return dataExpiracao;
    }

    public Instant getDataRevogacao() {
        return dataRevogacao;
    }
}
