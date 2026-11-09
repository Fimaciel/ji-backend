package br.edu.ifro.ji.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ji.mail.mode", havingValue = "test")
public class DisabledConviteMailSender implements ConviteMailSender {
    @Override
    public void enviar(String destinatario, String contexto, String remetente, String linkConvite) {
        // Adaptador sem efeito usado somente em testes que não exercitam o envio.
    }
}
