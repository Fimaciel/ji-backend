package br.edu.ifro.ji.service;

import br.edu.ifro.ji.dto.ConviteContextoResponse;
import br.edu.ifro.ji.exception.RegraNegocioException;
import br.edu.ifro.ji.model.ContextoParticipante;
import br.edu.ifro.ji.model.ConviteContexto;
import br.edu.ifro.ji.model.PapelParticipante;
import br.edu.ifro.ji.model.StatusConvite;
import br.edu.ifro.ji.model.Usuario;
import br.edu.ifro.ji.repository.ContextoParticipanteRepository;
import br.edu.ifro.ji.repository.ConviteContextoRepository;
import br.edu.ifro.ji.repository.DependenteRepository;
import br.edu.ifro.ji.repository.UsuarioRepository;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class ConviteContextoService {

    private final ConviteContextoRepository conviteRepository;
    private final ContextoParticipanteRepository participanteRepository;
    private final UsuarioRepository usuarioRepository;
    private final DependenteRepository dependenteRepository;
    private final TokenSeguroService tokenSeguroService;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public ConviteContextoService(ConviteContextoRepository conviteRepository,
                                  ContextoParticipanteRepository participanteRepository,
                                  UsuarioRepository usuarioRepository,
                                  DependenteRepository dependenteRepository,
                                  TokenSeguroService tokenSeguroService,
                                  AuditoriaService auditoriaService,
                                  Clock clock) {
        this.conviteRepository = conviteRepository;
        this.participanteRepository = participanteRepository;
        this.usuarioRepository = usuarioRepository;
        this.dependenteRepository = dependenteRepository;
        this.tokenSeguroService = tokenSeguroService;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
    }

    @Transactional
    public List<ConviteContextoResponse> listar(UsuarioPrincipal principal) {
        Instant agora = Instant.now(clock);
        List<ConviteContexto> convites = conviteRepository
                .findByEmailConvidadoIgnoreCaseOrderByDataCriacaoDesc(principal.email());
        convites.stream()
                .filter(convite -> convite.estaExpiradoEm(agora))
                .forEach(convite -> convite.expirar(agora));
        return convites.stream().map(ConviteContextoResponse::de).toList();
    }

    @Transactional
    public ConviteContextoResponse detalharPorToken(String token) {
        ConviteContexto convite = conviteRepository.findByTokenHash(tokenSeguroService.hash(token))
                .orElseThrow(this::conviteInvalido);
        expirarSeNecessario(convite);
        return ConviteContextoResponse.de(convite);
    }

    @Transactional
    public ConviteContextoResponse aceitar(UsuarioPrincipal principal, Long conviteId) {
        ConviteContexto convite = obterDoUsuario(principal, conviteId);
        validarPendente(convite);
        if (participanteRepository.existsByContextoIdAndUsuarioIdAndDataSaidaIsNull(
                convite.getContexto().getId(), principal.usuarioId())) {
            throw new RegraNegocioException(
                    HttpStatus.CONFLICT,
                    "participacao-existente",
                    "A conta já participa deste contexto."
            );
        }

        Usuario usuario = usuarioRepository.findById(principal.usuarioId())
                .orElseThrow(this::conviteInvalido);
        Instant agora = Instant.now(clock);
        ContextoParticipante participante = participanteRepository.save(new ContextoParticipante(
                convite.getContexto(),
                usuario,
                convite.getPapelProposto(),
                agora
        ));
        if (convite.getPapelProposto() == PapelParticipante.DEPENDENTE) {
            if (convite.getDependente() == null
                    || !convite.getDependente().getContexto().getId()
                            .equals(convite.getContexto().getId())
                    || convite.getDependente().getParticipante() != null) {
                throw new RegraNegocioException(
                        HttpStatus.CONFLICT,
                        "dependente-invalido",
                        "O dependente deste convite não está disponível para vínculo."
                );
            }
            convite.getDependente().vincularParticipante(participante, agora);
            dependenteRepository.save(convite.getDependente());
        }
        convite.aceitar(agora);
        auditoriaService.registrar(
                usuario,
                convite.getContexto(),
                "ACEITE_CONVITE",
                "CONVITE_CONTEXTO",
                convite.getId(),
                "Convite de contexto aceito."
        );
        return ConviteContextoResponse.de(convite);
    }

    @Transactional
    public ConviteContextoResponse recusar(UsuarioPrincipal principal, Long conviteId) {
        ConviteContexto convite = obterDoUsuario(principal, conviteId);
        validarPendente(convite);
        Instant agora = Instant.now(clock);
        convite.recusar(agora);
        Usuario usuario = usuarioRepository.findById(principal.usuarioId())
                .orElseThrow(this::conviteInvalido);
        auditoriaService.registrar(
                usuario,
                convite.getContexto(),
                "RECUSA_CONVITE",
                "CONVITE_CONTEXTO",
                convite.getId(),
                "Convite de contexto recusado."
        );
        return ConviteContextoResponse.de(convite);
    }

    private ConviteContexto obterDoUsuario(UsuarioPrincipal principal, Long conviteId) {
        ConviteContexto convite = conviteRepository.findDetalhadoById(conviteId)
                .orElseThrow(this::conviteInvalido);
        if (!convite.getEmailConvidado().equalsIgnoreCase(principal.email())) {
            throw conviteInvalido();
        }
        return convite;
    }

    private void validarPendente(ConviteContexto convite) {
        expirarSeNecessario(convite);
        if (convite.getStatus() != StatusConvite.PENDENTE) {
            throw new RegraNegocioException(
                    HttpStatus.BAD_REQUEST,
                    "convite-indisponivel",
                    "O convite expirou ou já foi respondido."
            );
        }
    }

    private void expirarSeNecessario(ConviteContexto convite) {
        Instant agora = Instant.now(clock);
        if (convite.estaExpiradoEm(agora)) {
            convite.expirar(agora);
        }
    }

    private RegraNegocioException conviteInvalido() {
        return new RegraNegocioException(
                HttpStatus.NOT_FOUND,
                "convite-invalido",
                "O convite informado não foi encontrado."
        );
    }
}
