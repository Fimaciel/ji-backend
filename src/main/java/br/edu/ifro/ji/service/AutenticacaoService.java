package br.edu.ifro.ji.service;

import br.edu.ifro.ji.dto.AutenticacaoResponse;
import br.edu.ifro.ji.dto.CadastroRequest;
import br.edu.ifro.ji.dto.ContextoResponse;
import br.edu.ifro.ji.dto.LoginRequest;
import br.edu.ifro.ji.dto.SessaoResponse;
import br.edu.ifro.ji.dto.UsuarioResponse;
import br.edu.ifro.ji.exception.RegraNegocioException;
import br.edu.ifro.ji.model.ConfiguracaoUsuario;
import br.edu.ifro.ji.model.Contexto;
import br.edu.ifro.ji.model.ContextoParticipante;
import br.edu.ifro.ji.model.PapelParticipante;
import br.edu.ifro.ji.model.Sessao;
import br.edu.ifro.ji.model.TipoContexto;
import br.edu.ifro.ji.model.Usuario;
import br.edu.ifro.ji.repository.ConfiguracaoUsuarioRepository;
import br.edu.ifro.ji.repository.ContextoParticipanteRepository;
import br.edu.ifro.ji.repository.ContextoRepository;
import br.edu.ifro.ji.repository.UsuarioRepository;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class AutenticacaoService {

    private final UsuarioRepository usuarioRepository;
    private final ConfiguracaoUsuarioRepository configuracaoRepository;
    private final ContextoRepository contextoRepository;
    private final ContextoParticipanteRepository participanteRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessaoService sessaoService;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public AutenticacaoService(UsuarioRepository usuarioRepository,
                               ConfiguracaoUsuarioRepository configuracaoRepository,
                               ContextoRepository contextoRepository,
                               ContextoParticipanteRepository participanteRepository,
                               PasswordEncoder passwordEncoder,
                               SessaoService sessaoService,
                               AuditoriaService auditoriaService,
                               Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.configuracaoRepository = configuracaoRepository;
        this.contextoRepository = contextoRepository;
        this.participanteRepository = participanteRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessaoService = sessaoService;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
    }

    @Transactional
    public UsuarioResponse cadastrar(CadastroRequest request) {
        String email = normalizarEmail(request.email());
        String cpf = request.cpf() == null || request.cpf().isBlank()
                ? null
                : request.cpf();
        if (usuarioRepository.findByEmailIgnoreCaseAndDataExclusaoIsNull(email).isPresent()) {
            throw new RegraNegocioException(
                    HttpStatus.CONFLICT,
                    "email-em-uso",
                    "O e-mail informado já está em uso."
            );
        }
        if (cpf != null && usuarioRepository.existsByCpf(cpf)) {
            throw new RegraNegocioException(
                    HttpStatus.CONFLICT,
                    "cpf-em-uso",
                    "O CPF informado já está em uso."
            );
        }

        Instant agora = Instant.now(clock);
        Usuario usuario = usuarioRepository.save(new Usuario(
                request.nome().trim(),
                request.sobrenome().trim(),
                cpf,
                request.dataNascimento(),
                email,
                passwordEncoder.encode(request.senha()),
                agora
        ));
        configuracaoRepository.save(new ConfiguracaoUsuario(usuario));

        Contexto contexto = contextoRepository.save(new Contexto(
                usuario,
                "Pessoal",
                TipoContexto.PESSOAL,
                agora
        ));
        participanteRepository.save(new ContextoParticipante(
                contexto,
                usuario,
                PapelParticipante.ADMINISTRADOR,
                agora
        ));

        return UsuarioResponse.de(usuario);
    }

    @Transactional
    public AutenticacaoResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository
                .findByEmailIgnoreCaseAndDataExclusaoIsNull(normalizarEmail(request.email()))
                .filter(encontrado -> passwordEncoder.matches(request.senha(), encontrado.getSenha()))
                .orElseThrow(this::credenciaisInvalidas);

        List<ContextoParticipante> participacoes = participanteRepository
                .findByUsuarioIdAndDataSaidaIsNullOrderByDataIngressoAsc(usuario.getId());
        if (participacoes.isEmpty()) {
            throw new RegraNegocioException(
                    HttpStatus.CONFLICT,
                    "usuario-sem-contexto",
                    "A conta não possui um contexto de gerenciamento ativo."
            );
        }

        Contexto contextoAtivo = participacoes.size() == 1
                ? participacoes.getFirst().getContexto()
                : null;
        SessaoService.NovaSessao novaSessao = sessaoService.criar(usuario, contextoAtivo);
        auditoriaService.registrar(
                usuario,
                contextoAtivo,
                "LOGIN",
                "SESSAO",
                novaSessao.sessao().getId(),
                "Login realizado com sucesso."
        );

        return new AutenticacaoResponse(
                novaSessao.token(),
                "Bearer",
                novaSessao.sessao().getDataExpiracao(),
                UsuarioResponse.de(usuario),
                contextoAtivo == null ? null : contextoAtivo.getId(),
                contextoAtivo == null,
                participacoes.stream().map(ContextoResponse::de).toList()
        );
    }

    @Transactional
    public void logout(UsuarioPrincipal principal) {
        Sessao sessao = sessaoService.revogar(principal.sessaoId());
        auditoriaService.registrar(
                sessao.getUsuario(),
                sessao.getContextoAtivo(),
                "LOGOUT",
                "SESSAO",
                sessao.getId(),
                "Sessão encerrada pelo usuário."
        );
    }

    @Transactional(readOnly = true)
    public SessaoResponse obterSessao(UsuarioPrincipal principal) {
        Sessao sessao = sessaoService.obter(principal.sessaoId());
        return new SessaoResponse(
                UsuarioResponse.de(sessao.getUsuario()),
                sessao.getContextoAtivo() == null ? null : sessao.getContextoAtivo().getId(),
                sessao.getDataExpiracao()
        );
    }

    private RegraNegocioException credenciaisInvalidas() {
        return new RegraNegocioException(
                HttpStatus.UNAUTHORIZED,
                "credenciais-invalidas",
                "E-mail ou senha inválidos."
        );
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
