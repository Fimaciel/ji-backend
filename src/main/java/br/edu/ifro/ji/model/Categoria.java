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
@Table(name = "categoria")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "contexto_id")
    private Contexto contexto;

    @Column(nullable = false, length = 60)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoCategoria tipo;

    @Column(name = "data_exclusao")
    private Instant dataExclusao;

    protected Categoria() {
    }

    public Categoria(Contexto contexto, String nome) {
        this.contexto = contexto;
        this.nome = nome;
        this.tipo = TipoCategoria.PERSONALIZADA;
    }

    public void renomear(String nome) {
        this.nome = nome;
    }

    public void excluir(Instant agora) {
        this.dataExclusao = agora;
    }

    public Long getId() {
        return id;
    }

    public Contexto getContexto() {
        return contexto;
    }

    public String getNome() {
        return nome;
    }

    public TipoCategoria getTipo() {
        return tipo;
    }

    public Instant getDataExclusao() {
        return dataExclusao;
    }

    public boolean isPadrao() {
        return tipo == TipoCategoria.PADRAO;
    }
}
