package br.edu.ifro.ji.service;

import br.edu.ifro.ji.dto.RedefinicaoSenhaRequest;
import br.edu.ifro.ji.exception.RegraNegocioException;
import br.edu.ifro.ji.model.TokenRedefinicaoSenha;
import br.edu.ifro.ji.model.Usuario;
import br.edu.ifro.ji.repository.SessaoRepository;
import br.edu.ifro.ji.repository.TokenRedefinicaoSenhaRepository;
import br.edu.ifro.ji.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecuperacaoSenhaServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-06T12:00:00Z");

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TokenRedefinicaoSenhaRepository tokenRepository;
    @Mock
    private SessaoRepository sessaoRepository;
    @Mock
    private TokenSeguroService tokenSeguroService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordResetMailSender mailSender;
    @Mock
    private AuditoriaService auditoriaService;

    private RecuperacaoSenhaService service;
    private Usuario usuario;

    @BeforeEach
    void configurar() {
        service = new RecuperacaoSenhaService(
                usuarioRepository, tokenRepository, sessaoRepository, tokenSeguroService,
                passwordEncoder, mailSender, auditoriaService,
                Clock.fixed(AGORA, ZoneOffset.UTC), "http://localhost:5173");
        usuario = new Usuario("Filipe", "Lopes", null, null,
                "filipe@example.com", "hash-antigo", AGORA.minusSeconds(10));
        ReflectionTestUtils.setField(usuario, "id", 1L);
    }

    @Test
    void solicitacaoParaEmailInexistenteNaoCriaTokenNemEnviaMensagem() {
        when(usuarioRepository.findByEmailIgnoreCaseAndDataExclusaoIsNull("ninguem@example.com"))
                .thenReturn(Optional.empty());

        service.solicitar(" NINGUEM@example.com ");

        verify(tokenRepository, never()).save(any());
        verify(mailSender, never()).enviar(any(), any(), any());
    }

    @Test
    void solicitacaoInvalidaTokensAnterioresEEnviaTokenDeTrintaMinutos() {
        when(usuarioRepository.findByEmailIgnoreCaseAndDataExclusaoIsNull("filipe@example.com"))
                .thenReturn(Optional.of(usuario));
        when(tokenSeguroService.gerar()).thenReturn("token-aberto");
        when(tokenSeguroService.hash("token-aberto")).thenReturn("a".repeat(64));
        when(tokenRepository.save(any(TokenRedefinicaoSenha.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.solicitar(" Filipe@Example.com ");

        verify(tokenRepository).invalidarTokensPendentes(1L);
        ArgumentCaptor<TokenRedefinicaoSenha> captor =
                ArgumentCaptor.forClass(TokenRedefinicaoSenha.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(Duration.between(captor.getValue().getDataCriacao(),
                captor.getValue().getDataExpiracao())).isEqualTo(Duration.ofMinutes(30));
        verify(mailSender).enviar(
                "filipe@example.com", "Filipe",
                "http://localhost:5173/redefinir-senha?token=token-aberto");
        verify(auditoriaService).registrar(
                usuario, null, "SOLICITACAO_REDEFINICAO_SENHA",
                "TOKEN_REDEFINICAO_SENHA", null, "Link de redefinição solicitado.");
    }

    @Test
    void redefinicaoRejeitaConfirmacaoDiferenteETokenInvalido() {
        assertThatThrownBy(() -> service.redefinir(
                new RedefinicaoSenhaRequest("token", "nova-senha", "outra-senha")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigo").isEqualTo("senhas-divergentes");

        when(tokenSeguroService.hash("token-invalido")).thenReturn("hash");
        when(tokenRepository.findByTokenHash("hash")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.redefinir(
                new RedefinicaoSenhaRequest("token-invalido", "nova-senha", "nova-senha")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigo").isEqualTo("token-redefinicao-invalido");
    }

    @Test
    void redefinicaoConsomeTokenTrocaBcryptRevogaSessoesEAudita() {
        TokenRedefinicaoSenha token = new TokenRedefinicaoSenha(
                usuario, "hash", AGORA.minusSeconds(1), AGORA.plusSeconds(1));
        when(tokenSeguroService.hash("token-aberto")).thenReturn("hash");
        when(tokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("nova-senha")).thenReturn("novo-bcrypt");

        service.redefinir(new RedefinicaoSenhaRequest(
                "token-aberto", "nova-senha", "nova-senha"));

        assertThat(usuario.getSenha()).isEqualTo("novo-bcrypt");
        assertThat(usuario.getDataAtualizacao()).isEqualTo(AGORA);
        assertThat(token.isUtilizado()).isTrue();
        verify(sessaoRepository).revogarTodasDoUsuario(1L, AGORA);
        verify(auditoriaService).registrar(
                usuario, null, "REDEFINICAO_SENHA", "USUARIO", 1L,
                "Senha redefinida e sessões anteriores revogadas.");
    }

    @Test
    void tokenExpiradoOuJaUtilizadoNaoPodeSerReutilizado() {
        TokenRedefinicaoSenha expirado = new TokenRedefinicaoSenha(
                usuario, "hash", AGORA.minusSeconds(60), AGORA);
        when(tokenSeguroService.hash("token")).thenReturn("hash");
        when(tokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(expirado));

        assertThatThrownBy(() -> service.redefinir(
                new RedefinicaoSenhaRequest("token", "nova-senha", "nova-senha")))
                .isInstanceOf(RegraNegocioException.class);
    }
}
