package br.edu.ifro.ji.service;

import br.edu.ifro.ji.dto.ContextoGerenciamentoResponse;
import br.edu.ifro.ji.dto.ContextoResponse;
import br.edu.ifro.ji.dto.CriarContextoRequest;
import br.edu.ifro.ji.exception.RegraNegocioException;
import br.edu.ifro.ji.model.Contexto;
import br.edu.ifro.ji.model.ContextoParticipante;
import br.edu.ifro.ji.model.PapelParticipante;
import br.edu.ifro.ji.model.TipoContexto;
import br.edu.ifro.ji.model.Usuario;
import br.edu.ifro.ji.repository.ContextoParticipanteRepository;
import br.edu.ifro.ji.repository.ContextoRepository;
import br.edu.ifro.ji.repository.UsuarioRepository;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class ContextoService {

    private final ContextoRepository contextoRepository;
    private final ContextoParticipanteRepository participanteRepository;
    private final UsuarioRepository usuarioRepository;
    private final SessaoService sessaoService;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public ContextoService(ContextoRepository contextoRepository,
                           ContextoParticipanteRepository participanteRepository,
                           UsuarioRepository usuarioRepository,
                           SessaoService sessaoService,
                           AuditoriaService auditoriaService,
                           Clock clock) {
        this.contextoRepository = contextoRepository;
        this.participanteRepository = participanteRepository;
        this.usuarioRepository = usuarioRepository;
        this.sessaoService = sessaoService;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ContextoGerenciamentoResponse> listar(UsuarioPrincipal principal) {
        return participanteRepository
                .findByUsuarioIdAndDataSaidaIsNullOrderByDataIngressoAsc(principal.usuarioId())
                .stream()
                .map(participante -> ContextoGerenciamentoResponse.de(
                        participante,
                        participanteRepository.countByContextoIdAndDataSaidaIsNull(
                                participante.getContexto().getId()),
                        principal.contextoAtivoId()))
                .toList();
    }

    @Transactional
    public ContextoGerenciamentoResponse criar(UsuarioPrincipal principal,
                                                CriarContextoRequest request) {
        Usuario usuario = usuarioRepository.findById(principal.usuarioId())
                .filter(Usuario::estaAtivo)
                .orElseThrow(this::usuarioInvalido);
        Instant agora = Instant.now(clock);
        Contexto contexto = contextoRepository.save(new Contexto(
                usuario,
                request.nome().trim(),
                TipoContexto.COMPARTILHADO,
                agora
        ));
        ContextoParticipante participante = participanteRepository.save(
                new ContextoParticipante(
                        contexto,
                        usuario,
                        PapelParticipante.ADMINISTRADOR,
                        agora
                ));
        auditoriaService.registrar(
                usuario,
                contexto,
                "CRIACAO_CONTEXTO",
                "CONTEXTO",
                contexto.getId(),
                "Contexto compartilhado criado."
        );
        return ContextoGerenciamentoResponse.de(
                participante,
                1,
                principal.contextoAtivoId()
        );
    }

    @Transactional
    public ContextoResponse selecionar(UsuarioPrincipal principal, Long contextoId) {
        ContextoParticipante participante = participanteRepository
                .findByContextoIdAndUsuarioIdAndDataSaidaIsNull(
                        contextoId,
                        principal.usuarioId())
                .orElseThrow(() -> new RegraNegocioException(
                        HttpStatus.NOT_FOUND,
                        "contexto-indisponivel",
                        "O contexto informado não está disponível para esta conta."
                ));
        sessaoService.selecionarContexto(principal.sessaoId(), participante.getContexto());
        auditoriaService.registrar(
                participante.getUsuario(),
                participante.getContexto(),
                "SELECAO_CONTEXTO",
                "SESSAO",
                principal.sessaoId(),
                "Contexto ativo da sessão alterado."
        );
        return ContextoResponse.de(participante);
    }

    private RegraNegocioException usuarioInvalido() {
        return new RegraNegocioException(
                HttpStatus.UNAUTHORIZED,
                "sessao-invalida",
                "A sessão é inválida ou expirou. Faça login novamente."
        );
    }
}
