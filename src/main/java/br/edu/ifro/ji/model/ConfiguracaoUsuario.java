package br.edu.ifro.ji.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "configuracao_usuario")
public class ConfiguracaoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "salario_mensal", nullable = false, precision = 12, scale = 2)
    private BigDecimal salarioMensal;

    @Column(nullable = false, length = 3)
    private String moeda;

    @Column(name = "notificacoes_ativas", nullable = false)
    private boolean notificacoesAtivas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Tema tema;

    protected ConfiguracaoUsuario() {
    }

    public ConfiguracaoUsuario(Usuario usuario) {
        this.usuario = usuario;
        this.salarioMensal = BigDecimal.ZERO;
        this.moeda = "BRL";
        this.notificacoesAtivas = true;
        this.tema = Tema.AUTOMATICO;
    }

    public void atualizar(BigDecimal salarioMensal, boolean notificacoesAtivas) {
        this.salarioMensal = salarioMensal;
        this.moeda = "BRL";
        this.notificacoesAtivas = notificacoesAtivas;
    }

    public void alterarTema(Tema tema) {
        this.tema = tema;
    }

    public void restaurarPadrao() {
        this.salarioMensal = BigDecimal.ZERO;
        this.moeda = "BRL";
        this.notificacoesAtivas = true;
        this.tema = Tema.AUTOMATICO;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public BigDecimal getSalarioMensal() {
        return salarioMensal;
    }

    public String getMoeda() {
        return moeda;
    }

    public boolean isNotificacoesAtivas() {
        return notificacoesAtivas;
    }

    public Tema getTema() {
        return tema;
    }
}
