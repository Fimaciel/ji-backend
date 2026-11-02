package br.edu.ifro.ji.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nome;

    @Column(nullable = false, length = 80)
    private String sobrenome;

    @Column(length = 11)
    private String cpf;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(nullable = false, length = 255)
    private String senha;

    @Column(name = "data_criacao", nullable = false)
    private Instant dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private Instant dataAtualizacao;

    @Column(name = "data_exclusao")
    private Instant dataExclusao;

    protected Usuario() {
    }

    public Usuario(String nome, String sobrenome, String cpf, LocalDate dataNascimento,
                   String email, String senha, Instant agora) {
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.cpf = cpf;
        this.dataNascimento = dataNascimento;
        this.email = email;
        this.senha = senha;
        this.dataCriacao = agora;
        this.dataAtualizacao = agora;
    }

    public void redefinirSenha(String novaSenha, Instant agora) {
        this.senha = novaSenha;
        this.dataAtualizacao = agora;
    }

    public void atualizarPerfil(String nome, String sobrenome, LocalDate dataNascimento,
                                String email, String novaSenha, Instant agora) {
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.dataNascimento = dataNascimento;
        this.email = email;
        if (novaSenha != null) {
            this.senha = novaSenha;
        }
        this.dataAtualizacao = agora;
    }

    public void registrarAtualizacao(Instant agora) {
        this.dataAtualizacao = agora;
    }

    public void encerrar(Instant agora) {
        if (dataExclusao == null) {
            this.dataExclusao = agora;
            this.dataAtualizacao = agora;
        }
    }

    public boolean estaAtivo() {
        return dataExclusao == null;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getSobrenome() {
        return sobrenome;
    }

    public String getCpf() {
        return cpf;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public String getEmail() {
        return email;
    }

    public String getSenha() {
        return senha;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }

    public Instant getDataAtualizacao() {
        return dataAtualizacao;
    }

    public Instant getDataExclusao() {
        return dataExclusao;
    }
}
