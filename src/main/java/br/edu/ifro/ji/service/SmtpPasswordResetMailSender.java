package br.edu.ifro.ji.service;

import br.edu.ifro.ji.exception.RegraNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ji.mail.mode", havingValue = "smtp")
public class SmtpPasswordResetMailSender implements PasswordResetMailSender {

    private final JavaMailSender mailSender;
    private final String remetente;

    public SmtpPasswordResetMailSender(
            JavaMailSender mailSender,
            @Value("${ji.mail.from:no-reply@ji.local}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    @Override
    public void enviar(String destinatario, String nome, String linkRedefinicao) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(destinatario);
        mensagem.setSubject("Redefinição de senha do Ji");
        mensagem.setText("Olá, " + nome + ".\n\n" +
                "Use o link abaixo para redefinir sua senha. Ele é válido por 30 minutos:\n" +
                linkRedefinicao + "\n\n" +
                "Se você não fez a solicitação, ignore esta mensagem.");
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
