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
@Table(name = "convite_contexto")
public class ConviteContexto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "contexto_id", nullable = false)
    private Contexto contexto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_remetente_id", nullable = false)
    private Usuario usuarioRemetente;

    @Column(name = "email_convidado", nullable = false, length = 120)
    private String emailConvidado;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel_proposto", nullable = false, length = 25)
    private PapelParticipante papelProposto;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatusConvite status;

    @Column(name = "data_criacao", nullable = false)
    private Instant dataCriacao;

    @Column(name = "data_expiracao", nullable = false)
    private Instant dataExpiracao;

    @Column(name = "data_resposta")
    private Instant dataResposta;

    protected ConviteContexto() {
    }

    public ConviteContexto(Contexto contexto, Usuario usuarioRemetente,
                           String emailConvidado, PapelParticipante papelProposto,
                           String tokenHash, Instant dataCriacao, Instant dataExpiracao) {
        this.contexto = contexto;
        this.usuarioRemetente = usuarioRemetente;
        this.emailConvidado = emailConvidado;
        this.papelProposto = papelProposto;
        this.tokenHash = tokenHash;
        this.status = StatusConvite.PENDENTE;
        this.dataCriacao = dataCriacao;
        this.dataExpiracao = dataExpiracao;
    }

    public boolean estaExpiradoEm(Instant instante) {
        return status == StatusConvite.PENDENTE && !dataExpiracao.isAfter(instante);
    }

    public void expirar(Instant instante) {
        if (status == StatusConvite.PENDENTE) {
            status = StatusConvite.EXPIRADO;
            dataResposta = instante;
        }
    }

    public void aceitar(Instant instante) {
        status = StatusConvite.ACEITO;
        dataResposta = instante;
    }

    public void recusar(Instant instante) {
        status = StatusConvite.RECUSADO;
        dataResposta = instante;
    }

    public Long getId() {
        return id;
    }

    public Contexto getContexto() {
        return contexto;
    }

    public Usuario getUsuarioRemetente() {
        return usuarioRemetente;
    }

    public String getEmailConvidado() {
        return emailConvidado;
    }

    public PapelParticipante getPapelProposto() {
        return papelProposto;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public StatusConvite getStatus() {
        return status;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }

    public Instant getDataExpiracao() {
        return dataExpiracao;
    }

    public Instant getDataResposta() {
        return dataResposta;
    }
}
