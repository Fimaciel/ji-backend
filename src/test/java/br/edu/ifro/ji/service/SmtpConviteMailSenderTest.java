package br.edu.ifro.ji.service;

import br.edu.ifro.ji.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpConviteMailSenderTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Test
    void enviaContextoRemetenteLinkEValidade() {
        SmtpConviteMailSender sender =
                new SmtpConviteMailSender(javaMailSender, "no-reply@ji.local");

        sender.enviar(
                "pessoa@example.com", "Família", "Filipe Lopes",
                "https://ji.local/convite/token-seguro"
        );

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertThat(captor.getValue().getFrom()).isEqualTo("no-reply@ji.local");
        assertThat(captor.getValue().getTo()).containsExactly("pessoa@example.com");
        assertThat(captor.getValue().getText())
                .contains("Família", "Filipe Lopes", "token-seguro", "sete dias");
    }

    @Test
    void converteFalhaDoSmtpEmErro503Estavel() {
        SmtpConviteMailSender sender =
                new SmtpConviteMailSender(javaMailSender, "no-reply@ji.local");
        doThrow(new MailSendException("SMTP fora do ar"))
                .when(javaMailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> sender.enviar(
                "pessoa@example.com", "Família", "Filipe Lopes",
                "https://ji.local/convite/token-seguro"
        ))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigo")
                .isEqualTo("servico-email-indisponivel");
    }
}
