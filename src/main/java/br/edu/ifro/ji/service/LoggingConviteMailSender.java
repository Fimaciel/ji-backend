package br.edu.ifro.ji.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ji.mail.mode", havingValue = "log", matchIfMissing = true)
public class LoggingConviteMailSender implements ConviteMailSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingConviteMailSender.class);

    @Override
    public void enviar(String destinatario, String contexto, String remetente, String linkConvite) {
        LOGGER.info("Convite do contexto {} preparado para {} por {}.", contexto, destinatario, remetente);
    }
}
