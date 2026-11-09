package br.edu.ifro.ji.service;

import br.edu.ifro.ji.exception.RegraNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ji.mail.mode", havingValue = "smtp")
public class SmtpConviteMailSender implements ConviteMailSender {

    private final JavaMailSender mailSender;
    private final String remetenteEmail;

    public SmtpConviteMailSender(JavaMailSender mailSender,
                                 @Value("${ji.mail.from:no-reply@ji.local}") String remetenteEmail) {
        this.mailSender = mailSender;
        this.remetenteEmail = remetenteEmail;
    }

    @Override
    public void enviar(String destinatario, String contexto, String remetente, String linkConvite) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetenteEmail);
        mensagem.setTo(destinatario);
        mensagem.setSubject("Convite para o contexto " + contexto + " no Ji");
        mensagem.setText(remetente + " convidou você para participar do contexto " + contexto
                + ".\n\nO link é válido por sete dias:\n" + linkConvite
                + "\n\nEntre com a conta correspondente a este e-mail para responder.");
        try {
            mailSender.send(mensagem);
        } catch (MailException exception) {
            throw new RegraNegocioException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "servico-email-indisponivel",
                    "Não foi possível enviar o e-mail neste momento. Tente novamente mais tarde."
            );
        }
    }
}
