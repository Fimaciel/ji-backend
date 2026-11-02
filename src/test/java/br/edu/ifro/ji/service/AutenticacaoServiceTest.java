package br.edu.ifro.ji.service;

import br.edu.ifro.ji.dto.CadastroRequest;
import br.edu.ifro.ji.dto.LoginRequest;
import br.edu.ifro.ji.exception.RegraNegocioException;
import br.edu.ifro.ji.model.ConfiguracaoUsuario;
import br.edu.ifro.ji.model.Contexto;
import br.edu.ifro.ji.model.ContextoParticipante;
import br.edu.ifro.ji.model.PapelParticipante;
import br.edu.ifro.ji.model.Sessao;
import br.edu.ifro.ji.model.Tema;
import br.edu.ifro.ji.model.TipoContexto;
import br.edu.ifro.ji.model.Usuario;
import br.edu.ifro.ji.repository.ConfiguracaoUsuarioRepository;
import br.edu.ifro.ji.repository.ContextoParticipanteRepository;
import br.edu.ifro.ji.repository.ContextoRepository;
import br.edu.ifro.ji.repository.UsuarioRepository;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-06T12:00:00Z");

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ConfiguracaoUsuarioRepository configuracaoRepository;
    @Mock
    private ContextoRepository contextoRepository;
    @Mock
    private ContextoParticipanteRepository participanteRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SessaoService sessaoService;
    @Mock
    private AuditoriaService auditoriaService;

    private AutenticacaoService service;

    @BeforeEach
    void configurar() {
        service = new AutenticacaoService(
                usuarioRepository,
                configuracaoRepository,
                contextoRepository,
                participanteRepository,
                passwordEncoder,
                sessaoService,
                auditoriaService,
                Clock.fixed(AGORA, ZoneOffset.UTC)
        );
    }

    @Test
    void cadastroNormalizaDadosECriaEstruturasIniciais() {
        var request = new CadastroRequest(
                " Filipe ", " Lopes ", "12345678901", null,
                " Filipe@Example.COM ", "senha-segura");
        when(usuarioRepository.findByEmailIgnoreCaseAndDataExclusaoIsNull("filipe@example.com"))
                .thenReturn(Optional.empty());
        when(usuarioRepository.existsByCpf("12345678901")).thenReturn(false);
        when(passwordEncoder.encode("senha-segura")).thenReturn("bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            ReflectionTestUtils.setField(usuario, "id", 1L);
            return usuario;
        });
        when(contextoRepository.save(any(Contexto.class))).thenAnswer(invocation -> {
            Contexto contexto = invocation.getArgument(0);
            ReflectionTestUtils.setField(contexto, "id", 2L);
            return contexto;
        });

        var response = service.cadastrar(request);

        assertThat(response.email()).isEqualTo("filipe@example.com");
        assertThat(response.nome()).isEqualTo("Filipe");
        ArgumentCaptor<ConfiguracaoUsuario> configuracao =
                ArgumentCaptor.forClass(ConfiguracaoUsuario.class);
        verify(configuracaoRepository).save(configuracao.capture());
        assertThat(configuracao.getValue().getMoeda()).isEqualTo("BRL");
        assertThat(configuracao.getValue().isNotificacoesAtivas()).isTrue();
        assertThat(configuracao.getValue().getTema()).isEqualTo(Tema.AUTOMATICO);

        ArgumentCaptor<ContextoParticipante> participante =
                ArgumentCaptor.forClass(ContextoParticipante.class);
        verify(participanteRepository).save(participante.capture());
        assertThat(participante.getValue().getPapel()).isEqualTo(PapelParticipante.ADMINISTRADOR);
        assertThat(participante.getValue().getContexto().getTipo()).isEqualTo(TipoContexto.PESSOAL);
    }

    @Test
    void cadastroRejeitaEmailOuCpfDuplicadoAntesDePersistir() {
        var request = new CadastroRequest(
                "Filipe", "Lopes", "12345678901", null,
                "filipe@example.com", "senha-segura");
        when(usuarioRepository.findByEmailIgnoreCaseAndDataExclusaoIsNull("filipe@example.com"))
                .thenReturn(Optional.of(usuario(1L)));

        assertThatThrownBy(() -> service.cadastrar(request))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigo").isEqualTo("email-em-uso");
        verify(usuarioRepository, never()).save(any());

        when(usuarioRepository.findByEmailIgnoreCaseAndDataExclusaoIsNull("outro@example.com"))
                .thenReturn(Optional.empty());
        when(usuarioRepository.existsByCpf("12345678901")).thenReturn(true);
        var outro = new CadastroRequest(
                "Outro", "Nome", "12345678901", null,
                "outro@example.com", "senha-segura");
        assertThatThrownBy(() -> service.cadastrar(outro))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigo").isEqualTo("cpf-em-uso");
    }

    @Test
    void loginAtivaAutomaticamenteOUnicoContextoERegistraAuditoria() {
        Usuario usuario = usuario(1L);
        Contexto contexto = contexto(usuario, 2L, "Pessoal");
        ContextoParticipante participante = new ContextoParticipante(
                contexto, usuario, PapelParticipante.ADMINISTRADOR, AGORA);
        Sessao sessao = new Sessao(usuario, contexto, UUID.randomUUID(),
                AGORA, AGORA.plusSeconds(3600));
        ReflectionTestUtils.setField(sessao, "id", 3L);
        when(usuarioRepository.findByEmailIgnoreCaseAndDataExclusaoIsNull("filipe@example.com"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-segura", "bcrypt")).thenReturn(true);
        when(participanteRepository.findByUsuarioIdAndDataSaidaIsNullOrderByDataIngressoAsc(1L))
                .thenReturn(List.of(participante));
        when(sessaoService.criar(usuario, contexto))
                .thenReturn(new SessaoService.NovaSessao("jwt", sessao));

        var response = service.login(new LoginRequest(" Filipe@EXAMPLE.com ", "senha-segura"));

        assertThat(response.tokenAcesso()).isEqualTo("jwt");
        assertThat(response.contextoAtivoId()).isEqualTo(2L);
        assertThat(response.selecaoContextoNecessaria()).isFalse();
        assertThat(response.contextos()).hasSize(1);
        verify(auditoriaService).registrar(
                usuario, contexto, "LOGIN", "SESSAO", 3L, "Login realizado com sucesso.");
    }

    @Test
    void loginComMaisDeUmContextoSinalizaSelecaoSemAtivarUmDeles() {
        Usuario usuario = usuario(1L);
        Contexto primeiro = contexto(usuario, 2L, "Pessoal");
        Contexto segundo = contexto(usuario, 4L, "Família");
        when(usuarioRepository.findByEmailIgnoreCaseAndDataExclusaoIsNull("filipe@example.com"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha", "bcrypt")).thenReturn(true);
        when(participanteRepository.findByUsuarioIdAndDataSaidaIsNullOrderByDataIngressoAsc(1L))
                .thenReturn(List.of(
                        new ContextoParticipante(primeiro, usuario, PapelParticipante.ADMINISTRADOR, AGORA),
                        new ContextoParticipante(segundo, usuario, PapelParticipante.COLABORADOR, AGORA)));
        Sessao sessao = new Sessao(usuario, null, UUID.randomUUID(), AGORA, AGORA.plusSeconds(10));
        when(sessaoService.criar(usuario, null))
                .thenReturn(new SessaoService.NovaSessao("jwt", sessao));

        var response = service.login(new LoginRequest("filipe@example.com", "senha"));

        assertThat(response.selecaoContextoNecessaria()).isTrue();
        assertThat(response.contextoAtivoId()).isNull();
        assertThat(response.contextos()).hasSize(2);
    }

    @Test
    void loginRejeitaCredenciaisInvalidasEContaSemContexto() {
        when(usuarioRepository.findByEmailIgnoreCaseAndDataExclusaoIsNull("inexistente@example.com"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.login(
                new LoginRequest("inexistente@example.com", "senha")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigo").isEqualTo("credenciais-invalidas");

        Usuario usuario = usuario(1L);
        when(usuarioRepository.findByEmailIgnoreCaseAndDataExclusaoIsNull("filipe@example.com"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha", "bcrypt")).thenReturn(true);
        when(participanteRepository.findByUsuarioIdAndDataSaidaIsNullOrderByDataIngressoAsc(1L))
                .thenReturn(List.of());
        assertThatThrownBy(() -> service.login(new LoginRequest("filipe@example.com", "senha")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigo").isEqualTo("usuario-sem-contexto");
    }

    @Test
    void logoutRevogaSessaoEAuditaESessaoAtualRetornaValidade() {
        Usuario usuario = usuario(1L);
        Contexto contexto = contexto(usuario, 2L, "Pessoal");
        Sessao sessao = new Sessao(usuario, contexto, UUID.randomUUID(),
                AGORA, AGORA.plusSeconds(3600));
        ReflectionTestUtils.setField(sessao, "id", 3L);
        UsuarioPrincipal principal = new UsuarioPrincipal(1L, usuario.getEmail(), 3L, 2L);
        when(sessaoService.revogar(3L)).thenReturn(sessao);
        when(sessaoService.obter(3L)).thenReturn(sessao);

        var resposta = service.obterSessao(principal);
        service.logout(principal);

        assertThat(resposta.usuario().id()).isEqualTo(1L);
        assertThat(resposta.contextoAtivoId()).isEqualTo(2L);
        assertThat(resposta.expiraEm()).isEqualTo(AGORA.plusSeconds(3600));
        verify(auditoriaService).registrar(
                usuario, contexto, "LOGOUT", "SESSAO", 3L,
                "Sessão encerrada pelo usuário.");
    }

    private Usuario usuario(Long id) {
        Usuario usuario = new Usuario("Filipe", "Lopes", null, null,
                "filipe@example.com", "bcrypt", AGORA);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    private Contexto contexto(Usuario usuario, Long id, String nome) {
        Contexto contexto = new Contexto(usuario, nome, TipoContexto.PESSOAL, AGORA);
        ReflectionTestUtils.setField(contexto, "id", id);
        return contexto;
    }
}
