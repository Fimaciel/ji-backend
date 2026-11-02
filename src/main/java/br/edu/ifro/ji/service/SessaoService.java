package br.edu.ifro.ji.service;

import br.edu.ifro.ji.exception.RegraNegocioException;
import br.edu.ifro.ji.model.Contexto;
import br.edu.ifro.ji.model.Sessao;
import br.edu.ifro.ji.model.Usuario;
import br.edu.ifro.ji.repository.ContextoParticipanteRepository;
import br.edu.ifro.ji.repository.SessaoRepository;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class SessaoService {

    private final SessaoRepository repository;
    private final ContextoParticipanteRepository participanteRepository;
    private final JwtTokenService jwtTokenService;
    private final Clock clock;

    public SessaoService(SessaoRepository repository,
                         ContextoParticipanteRepository participanteRepository,
                         JwtTokenService jwtTokenService,
                         Clock clock) {
        this.repository = repository;
        this.participanteRepository = participanteRepository;
        this.jwtTokenService = jwtTokenService;
        this.clock = clock;
    }

    @Transactional
    public NovaSessao criar(Usuario usuario, Contexto contextoAtivo) {
        JwtTokenService.TokenEmitido token = jwtTokenService.emitir(usuario.getId());
        Sessao sessao = repository.save(new Sessao(
                usuario,
                contextoAtivo,
                token.jti(),
                token.emitidoEm(),
                token.expiraEm()
        ));
        return new NovaSessao(token.valor(), sessao);
    }

    @Transactional(readOnly = true)
    public UsuarioPrincipal autenticar(Jwt jwt) {
        UUID jti;
        Long usuarioId;
        try {
            jti = UUID.fromString(jwt.getId());
            usuarioId = Long.valueOf(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw sessaoInvalida();
        }

        Sessao sessao = buscarAtiva(jti);
        if (!sessao.getUsuario().getId().equals(usuarioId)) {
            throw sessaoInvalida();
        }
        Long contextoId = sessao.getContextoAtivo() == null
                ? null
                : sessao.getContextoAtivo().getId();
        if (contextoId != null && !participanteRepository
                .existsByContextoIdAndUsuarioIdAndDataSaidaIsNull(contextoId, usuarioId)) {
            throw sessaoInvalida();
        }
        return new UsuarioPrincipal(
                usuarioId,
                sessao.getUsuario().getEmail(),
                sessao.getId(),
                contextoId
        );
    }

    @Transactional
    public Sessao revogar(Long sessaoId) {
        Instant agora = Instant.now(clock);
        Sessao sessao = repository.findById(sessaoId)
                .orElseThrow(this::sessaoInvalida);
        if (!sessao.estaAtivaEm(agora)) {
            throw sessaoInvalida();
        }
        sessao.revogar(agora);
        return sessao;
    }

    @Transactional(readOnly = true)
    public Sessao obter(Long sessaoId) {
        Instant agora = Instant.now(clock);
        return repository.findById(sessaoId)
                .filter(sessao -> sessao.estaAtivaEm(agora))
                .orElseThrow(this::sessaoInvalida);
    }

    private Sessao buscarAtiva(UUID jti) {
        Instant agora = Instant.now(clock);
        return repository.findByJti(jti)
                .filter(sessao -> sessao.estaAtivaEm(agora))
                .orElseThrow(this::sessaoInvalida);
    }

    private RegraNegocioException sessaoInvalida() {
        return new RegraNegocioException(
                HttpStatus.UNAUTHORIZED,
                "sessao-invalida",
                "A sessão é inválida ou expirou. Faça login novamente."
        );
    }

    public record NovaSessao(String token, Sessao sessao) {
    }
}
