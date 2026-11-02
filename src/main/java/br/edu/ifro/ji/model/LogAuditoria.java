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
@Table(name = "log_auditoria")
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "contexto_id")
    private Contexto contexto;

    @Column(nullable = false, length = 40)
    private String acao;

    @Column(name = "entidade_afetada", nullable = false, length = 40)
    private String entidadeAfetada;

    @Column(name = "registro_id", nullable = false)
    private Long registroId;

    @Column(name = "data_hora", nullable = false)
    private Instant dataHora;

    @Column(nullable = false, length = 255)
    private String detalhes;

    protected LogAuditoria() {
    }

    public LogAuditoria(Usuario usuario, Contexto contexto, String acao,
                        String entidadeAfetada, Long registroId,
                        Instant dataHora, String detalhes) {
        this.usuario = usuario;
        this.contexto = contexto;
        this.acao = acao;
        this.entidadeAfetada = entidadeAfetada;
        this.registroId = registroId;
        this.dataHora = dataHora;
        this.detalhes = detalhes;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Contexto getContexto() {
        return contexto;
    }

    public String getAcao() {
        return acao;
    }

    public String getEntidadeAfetada() {
        return entidadeAfetada;
    }

    public Long getRegistroId() {
        return registroId;
    }

    public Instant getDataHora() {
        return dataHora;
    }

    public String getDetalhes() {
        return detalhes;
    }
}
