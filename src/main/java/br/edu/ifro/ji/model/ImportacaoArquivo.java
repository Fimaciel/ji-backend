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
@Table(name = "importacao_arquivo")
public class ImportacaoArquivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "contexto_id", nullable = false)
    private Contexto contexto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "nome_arquivo", nullable = false, length = 255)
    private String nomeArquivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FormatoImportacao formato;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatusImportacao status;

    @Column(name = "total_lidos", nullable = false)
    private int totalLidos;

    @Column(name = "total_importados", nullable = false)
    private int totalImportados;

    @Column(name = "total_duplicados", nullable = false)
    private int totalDuplicados;

    @Column(name = "data_criacao", nullable = false)
    private Instant dataCriacao;

    @Column(name = "data_desfeita")
    private Instant dataDesfeita;

    protected ImportacaoArquivo() {
    }

    public ImportacaoArquivo(Contexto contexto, Usuario usuario, String nomeArquivo,
                             FormatoImportacao formato, int totalLidos,
                             int totalImportados, int totalDuplicados, Instant agora) {
        this.contexto = contexto;
        this.usuario = usuario;
        this.nomeArquivo = nomeArquivo;
        this.formato = formato;
        this.status = StatusImportacao.CONCLUIDA;
        this.totalLidos = totalLidos;
        this.totalImportados = totalImportados;
        this.totalDuplicados = totalDuplicados;
        this.dataCriacao = agora;
    }

    public void desfazer(Instant agora) {
        this.status = StatusImportacao.DESFEITA;
        this.dataDesfeita = agora;
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

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public FormatoImportacao getFormato() {
        return formato;
    }

    public StatusImportacao getStatus() {
        return status;
    }

    public int getTotalLidos() {
        return totalLidos;
    }

    public int getTotalImportados() {
        return totalImportados;
    }

    public int getTotalDuplicados() {
        return totalDuplicados;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }

    public Instant getDataDesfeita() {
        return dataDesfeita;
    }
}
