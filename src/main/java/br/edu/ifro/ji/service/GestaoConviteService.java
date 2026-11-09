package br.edu.ifro.ji.service;

import br.edu.ifro.ji.dto.ConvidarContextoRequest;
import br.edu.ifro.ji.dto.ConviteEnviadoResponse;
import br.edu.ifro.ji.exception.RegraNegocioException;
import br.edu.ifro.ji.model.Contexto;
import br.edu.ifro.ji.model.ConviteContexto;
import br.edu.ifro.ji.model.Dependente;
import br.edu.ifro.ji.model.PapelParticipante;
import br.edu.ifro.ji.model.StatusConvite;
import br.edu.ifro.ji.model.TipoContexto;
import br.edu.ifro.ji.model.Usuario;
import br.edu.ifro.ji.repository.ContextoParticipanteRepository;
import br.edu.ifro.ji.repository.ConviteContextoRepository;
import br.edu.ifro.ji.repository.UsuarioRepository;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class GestaoConviteService {

    private static final Duration VALIDADE = Duration.ofDays(7);

    private final ContextoAtivoService contextoAtivoService;
    private final ContextoParticipanteRepository participanteRepository;
    private final ConviteContextoRepository conviteRepository;
    private final UsuarioRepository usuarioRepository;
    private final TokenSeguroService tokenSeguroService;
    private final ConviteMailSender mailSender;
    private final AuditoriaService auditoriaService;
    private final Clock clock;
    private final String frontendUrl;

    public GestaoConviteService(ContextoAtivoService contextoAtivoService,
                                ContextoParticipanteRepository participanteRepository,
                                ConviteContextoRepository conviteRepository,
                                UsuarioRepository usuarioRepository,
                                TokenSeguroService tokenSeguroService,
                                ConviteMailSender mailSender,
                                AuditoriaService auditoriaService,
                                Clock clock,
                                @Value("${ji.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.contextoAtivoService = contextoAtivoService;
        this.participanteRepository = participanteRepository;
        this.conviteRepository = conviteRepository;
        this.usuarioRepository = usuarioRepository;
        this.tokenSeguroService = tokenSeguroService;
        this.mailSender = mailSender;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public ConviteEnviadoResponse convidarColaborador(UsuarioPrincipal principal,
                                                       ConvidarContextoRequest request) {
        ContextoAtivoService.AcessoContexto acesso = obterAdministrador(principal, true);
        String email = normalizarEmail(request.email());
        validarNovoDestinatario(acesso.contexto(), email);
        ConviteContexto convite = criar(
                acesso.contexto(), acesso.usuario(), email,
                PapelParticipante.COLABORADOR, null
        );
        return ConviteEnviadoResponse.de(convite);
    }

    @Transactional
    public ConviteContexto criarParaDependente(Contexto contexto, Usuario remetente,
                                                String emailInformado, Dependente dependente) {
        String email = normalizarEmail(emailInformado);
        validarNovoDestinatario(contexto, email);
        return criar(contexto, remetente, email, PapelParticipante.DEPENDENTE, dependente);
    }

    @Transactional
    public List<ConviteEnviadoResponse> listarEnviados(UsuarioPrincipal principal) {
        ContextoAtivoService.AcessoContexto acesso = obterAdministrador(principal, false);
        Instant agora = Instant.now(clock);
        List<ConviteContexto> convites = conviteRepository
                .findByContextoIdOrderByDataCriacaoDesc(acesso.contexto().getId());
        convites.stream().filter(convite -> convite.estaExpiradoEm(agora))
                .forEach(convite -> convite.expirar(agora));
        return convites.stream().map(ConviteEnviadoResponse::de).toList();
    }

    @Transactional
    public ConviteEnviadoResponse reenviar(UsuarioPrincipal principal, Long conviteId) {
        ContextoAtivoService.AcessoContexto acesso = obterAdministrador(principal, false);
        ConviteContexto convite = obterDoContexto(conviteId, acesso.contexto().getId());
        if (convite.getStatus() != StatusConvite.PENDENTE) {
            throw indisponivel();
        }
        String token = tokenSeguroService.gerar();
        Instant agora = Instant.now(clock);
        convite.reenviar(tokenSeguroService.hash(token), agora, agora.plus(VALIDADE));
        enviar(convite, token);
        auditar(acesso.usuario(), convite, "REENVIO_CONVITE", "Convite reenviado.");
        return ConviteEnviadoResponse.de(convite);
    }

    @Transactional
    public void cancelar(UsuarioPrincipal principal, Long conviteId) {
        ContextoAtivoService.AcessoContexto acesso = obterAdministrador(principal, false);
        ConviteContexto convite = obterDoContexto(conviteId, acesso.contexto().getId());
        if (convite.getStatus() != StatusConvite.PENDENTE) {
            throw indisponivel();
        }
        convite.cancelar(Instant.now(clock));
        auditar(acesso.usuario(), convite, "CANCELAMENTO_CONVITE", "Convite cancelado.");
    }

    private ConviteContexto criar(Contexto contexto, Usuario remetente, String email,
                                  PapelParticipante papel, Dependente dependente) {
        String token = tokenSeguroService.gerar();
        Instant agora = Instant.now(clock);
        ConviteContexto convite = dependente == null
                ? new ConviteContexto(contexto, remetente, email, papel,
                        tokenSeguroService.hash(token), agora, agora.plus(VALIDADE))
                : ConviteContexto.paraDependente(contexto, remetente, email, dependente,
                        tokenSeguroService.hash(token), agora, agora.plus(VALIDADE));
        conviteRepository.save(convite);
        enviar(convite, token);
        auditar(remetente, convite, "ENVIO_CONVITE",
                "Convite enviado para " + email + " com papel " + papel + ".");
        return convite;
    }

    private void validarNovoDestinatario(Contexto contexto, String email) {
        Instant agora = Instant.now(clock);
        conviteRepository.findByContextoIdAndEmailConvidadoIgnoreCaseAndStatus(
                contexto.getId(), email, StatusConvite.PENDENTE
        ).ifPresent(convite -> {
            if (convite.estaExpiradoEm(agora)) {
                convite.expirar(agora);
                conviteRepository.flush();
                return;
            }
            throw new RegraNegocioException(
                    HttpStatus.CONFLICT,
                    "convite-pendente-existente",
                    "Já existe um convite pendente para este e-mail. Reenvie ou cancele o convite atual."
            );
        });

        usuarioRepository.findByEmailIgnoreCaseAndDataExclusaoIsNull(email).ifPresent(usuario -> {
            if (participanteRepository.existsByContextoIdAndUsuarioIdAndDataSaidaIsNull(
                    contexto.getId(), usuario.getId())) {
                throw new RegraNegocioException(
                        HttpStatus.CONFLICT,
                        "participacao-existente",
                        "Este usuário já participa do contexto."
                );
            }
        });
    }

    private ContextoAtivoService.AcessoContexto obterAdministrador(UsuarioPrincipal principal,
                                                                    boolean exigirCompartilhado) {
        ContextoAtivoService.AcessoContexto acesso = contextoAtivoService.obterParaLeitura(principal);
        if (acesso.papel() != PapelParticipante.ADMINISTRADOR
                || (exigirCompartilhado && acesso.contexto().getTipo() != TipoContexto.COMPARTILHADO)) {
            throw new RegraNegocioException(
                    HttpStatus.FORBIDDEN,
                    "administrador-obrigatorio",
                    "Apenas o administrador de um contexto compartilhado pode realizar esta operação."
            );
        }
        return acesso;
    }

    private ConviteContexto obterDoContexto(Long conviteId, Long contextoId) {
        return conviteRepository.findByIdAndContextoId(conviteId, contextoId)
                .orElseThrow(() -> new RegraNegocioException(
                        HttpStatus.NOT_FOUND, "convite-invalido", "O convite informado não foi encontrado."
                ));
    }

    private void enviar(ConviteContexto convite, String token) {
        Usuario remetente = convite.getUsuarioRemetente();
        mailSender.enviar(
                convite.getEmailConvidado(),
                convite.getContexto().getNome(),
                (remetente.getNome() + " " + remetente.getSobrenome()).trim(),
                frontendUrl + "/convite/" + token
        );
    }

    private void auditar(Usuario usuario, ConviteContexto convite, String acao, String detalhes) {
        auditoriaService.registrar(
                usuario, convite.getContexto(), acao,
                "CONVITE_CONTEXTO", convite.getId(), detalhes
        );
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private RegraNegocioException indisponivel() {
        return new RegraNegocioException(
                HttpStatus.CONFLICT, "convite-indisponivel", "Este convite não está mais pendente."
        );
    }
}
