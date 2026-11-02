package br.edu.ifro.ji.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ji.mail.mode", havingValue = "log", matchIfMissing = true)
public class LoggingPasswordResetMailSender implements PasswordResetMailSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingPasswordResetMailSender.class);

    @Override
    public void enviar(String destinatario, String nome, String linkRedefinicao) {
        LOGGER.info("E-mail de redefinição destinado a {}: {}", destinatario, linkRedefinicao);
    }
}
