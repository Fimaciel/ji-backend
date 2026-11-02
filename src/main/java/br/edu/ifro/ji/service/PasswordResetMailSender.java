package br.edu.ifro.ji.service;

public interface PasswordResetMailSender {

    void enviar(String destinatario, String nome, String linkRedefinicao);
}
