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

@Entity
@Table(name = "token_redefinicao_senha")
public class TokenRedefinicaoSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "data_criacao", nullable = false)
    private Instant dataCriacao;

    @Column(name = "data_expiracao", nullable = false)
    private Instant dataExpiracao;

    @Column(nullable = false)
    private boolean utilizado;

    protected TokenRedefinicaoSenha() {
    }

    public TokenRedefinicaoSenha(Usuario usuario, String tokenHash,
                                 Instant dataCriacao, Instant dataExpiracao) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.dataCriacao = dataCriacao;
        this.dataExpiracao = dataExpiracao;
    }

    public boolean estaValidoEm(Instant instante) {
        return !utilizado && dataExpiracao.isAfter(instante);
    }

    public void marcarComoUtilizado() {
        this.utilizado = true;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }

    public Instant getDataExpiracao() {
        return dataExpiracao;
    }

    public boolean isUtilizado() {
        return utilizado;
    }
}
