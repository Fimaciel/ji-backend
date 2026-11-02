package br.edu.ifro.ji.service;

import br.edu.ifro.ji.model.LogAuditoria;
import br.edu.ifro.ji.model.Usuario;
import br.edu.ifro.ji.repository.LogAuditoriaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock
    private LogAuditoriaRepository repository;

    @Test
    void registraEventoComHorarioUtcEIdentificacaoDoRegistro() {
        Instant agora = Instant.parse("2026-10-06T12:00:00Z");
        AuditoriaService service = new AuditoriaService(
                repository, Clock.fixed(agora, ZoneOffset.UTC));
        Usuario usuario = new Usuario(
                "Filipe", "Lopes", null, null, "filipe@example.com", "hash", agora);

        service.registrar(usuario, null, "LOGIN", "SESSAO", 10L, "Login realizado.");

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isEqualTo(usuario);
        assertThat(captor.getValue().getAcao()).isEqualTo("LOGIN");
        assertThat(captor.getValue().getEntidadeAfetada()).isEqualTo("SESSAO");
        assertThat(captor.getValue().getRegistroId()).isEqualTo(10L);
        assertThat(captor.getValue().getDataHora()).isEqualTo(agora);
    }
}
