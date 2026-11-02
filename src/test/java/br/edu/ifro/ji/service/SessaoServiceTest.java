package br.edu.ifro.ji.service;

import br.edu.ifro.ji.exception.RegraNegocioException;
import br.edu.ifro.ji.model.Contexto;
import br.edu.ifro.ji.model.Sessao;
import br.edu.ifro.ji.model.TipoContexto;
import br.edu.ifro.ji.model.Usuario;
import br.edu.ifro.ji.repository.ContextoParticipanteRepository;
import br.edu.ifro.ji.repository.SessaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessaoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-06T12:00:00Z");

    @Mock
    private SessaoRepository repository;
    @Mock
    private ContextoParticipanteRepository participanteRepository;
    @Mock
    private JwtTokenService jwtTokenService;

    private SessaoService service;
    private Usuario usuario;
    private Contexto contexto;

    @BeforeEach
    void configurar() {
        service = new SessaoService(repository, participanteRepository, jwtTokenService,
                Clock.fixed(AGORA, ZoneOffset.UTC));
        usuario = new Usuario("Filipe", "Lopes", null, null,
                "filipe@example.com", "hash", AGORA);
        ReflectionTestUtils.setField(usuario, "id", 10L);
        contexto = new Contexto(usuario, "Pessoal", TipoContexto.PESSOAL, AGORA);
        ReflectionTestUtils.setField(contexto, "id", 20L);
    }

    @Test
    void criaSessaoLigadaAoJtiDoJwt() {
        UUID jti = UUID.randomUUID();
        var token = new JwtTokenService.TokenEmitido(
                "jwt", jti, AGORA, AGORA.plusSeconds(3600));
        when(jwtTokenService.emitir(10L)).thenReturn(token);
        when(repository.save(org.mockito.ArgumentMatchers.any(Sessao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = service.criar(usuario, contexto);

        assertThat(resultado.token()).isEqualTo("jwt");
        assertThat(resultado.sessao().getJti()).isEqualTo(jti);
        assertThat(resultado.sessao().getContextoAtivo()).isEqualTo(contexto);
    }

    @Test
    void autenticaJwtQuandoSessaoEParticipacaoEstaoAtivas() {
        UUID jti = UUID.randomUUID();
        Sessao sessao = sessao(jti, contexto, AGORA.plusSeconds(3600));
        ReflectionTestUtils.setField(sessao, "id", 30L);
        when(repository.findByJti(jti)).thenReturn(Optional.of(sessao));
        when(participanteRepository.existsByContextoIdAndUsuarioIdAndDataSaidaIsNull(20L, 10L))
                .thenReturn(true);

        var principal = service.autenticar(jwt(jti, "10"));

        assertThat(principal.usuarioId()).isEqualTo(10L);
        assertThat(principal.sessaoId()).isEqualTo(30L);
        assertThat(principal.contextoAtivoId()).isEqualTo(20L);
    }

    @Test
    void rejeitaJwtMalformadoSessaoExpiradaOuParticipacaoEncerrada() {
        assertThatThrownBy(() -> service.autenticar(jwt("jti-invalido", "10")))
                .isInstanceOf(RegraNegocioException.class);

        UUID expirado = UUID.randomUUID();
        when(repository.findByJti(expirado))
                .thenReturn(Optional.of(sessao(expirado, contexto, AGORA)));
        assertThatThrownBy(() -> service.autenticar(jwt(expirado, "10")))
                .isInstanceOf(RegraNegocioException.class);

        UUID semParticipacao = UUID.randomUUID();
        when(repository.findByJti(semParticipacao))
                .thenReturn(Optional.of(sessao(semParticipacao, contexto, AGORA.plusSeconds(1))));
        when(participanteRepository.existsByContextoIdAndUsuarioIdAndDataSaidaIsNull(20L, 10L))
                .thenReturn(false);
        assertThatThrownBy(() -> service.autenticar(jwt(semParticipacao, "10")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void rejeitaQuandoSubjectNaoCorrespondeAoDonoDaSessao() {
        UUID jti = UUID.randomUUID();
        when(repository.findByJti(jti))
                .thenReturn(Optional.of(sessao(jti, null, AGORA.plusSeconds(1))));

        assertThatThrownBy(() -> service.autenticar(jwt(jti, "99")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void revogaSessaoAtivaEConsultaPorId() {
        Sessao sessao = sessao(UUID.randomUUID(), contexto, AGORA.plusSeconds(1));
        when(repository.findById(30L)).thenReturn(Optional.of(sessao));

        assertThat(service.obter(30L)).isEqualTo(sessao);
        assertThat(service.revogar(30L)).isEqualTo(sessao);
        assertThat(sessao.getDataRevogacao()).isEqualTo(AGORA);
        verify(repository, org.mockito.Mockito.times(2)).findById(30L);
    }

    private Sessao sessao(UUID jti, Contexto contextoAtivo, Instant expiracao) {
        return new Sessao(usuario, contextoAtivo, jti, AGORA.minusSeconds(1), expiracao);
    }

    private Jwt jwt(UUID jti, String subject) {
        return jwt(jti.toString(), subject);
    }

    private Jwt jwt(String jti, String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(subject)
                .claim("jti", jti)
                .issuedAt(AGORA.minusSeconds(1))
                .expiresAt(AGORA.plusSeconds(1))
                .build();
    }
}
