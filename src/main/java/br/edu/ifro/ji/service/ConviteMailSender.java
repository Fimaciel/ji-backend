package br.edu.ifro.ji.service;

public interface ConviteMailSender {
    void enviar(String destinatario, String contexto, String remetente, String linkConvite);
}
