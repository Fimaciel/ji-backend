package br.edu.ifro.ji.service;

import br.edu.ifro.ji.dto.ParticipanteContextoResponse;
import br.edu.ifro.ji.exception.RegraNegocioException;
import br.edu.ifro.ji.model.ContextoParticipante;
import br.edu.ifro.ji.model.PapelParticipante;
import br.edu.ifro.ji.model.TipoContexto;
import br.edu.ifro.ji.repository.ContextoParticipanteRepository;
import br.edu.ifro.ji.repository.DependenteRepository;
import br.edu.ifro.ji.repository.SessaoRepository;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class ParticipanteContextoService {

    private final ContextoAtivoService contextoAtivoService;
    private final ContextoParticipanteRepository participanteRepository;
    private final DependenteRepository dependenteRepository;
    private final SessaoRepository sessaoRepository;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public ParticipanteContextoService(ContextoAtivoService contextoAtivoService,
                                       ContextoParticipanteRepository participanteRepository,
                                       DependenteRepository dependenteRepository,
                                       SessaoRepository sessaoRepository,
                                       AuditoriaService auditoriaService,
                                       Clock clock) {
        this.contextoAtivoService = contextoAtivoService;
        this.participanteRepository = participanteRepository;
        this.dependenteRepository = dependenteRepository;
        this.sessaoRepository = sessaoRepository;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ParticipanteContextoResponse> listar(UsuarioPrincipal principal) {
        ContextoAtivoService.AcessoContexto acesso = obterCompartilhado(principal);
        return participanteRepository.findByContextoIdOrderByDataIngressoAsc(acesso.contexto().getId())
                .stream().map(ParticipanteContextoResponse::de).toList();
    }

    @Transactional
    public ParticipanteContextoResponse alterarPapel(UsuarioPrincipal principal,
                                                      Long participanteId,
                                                      PapelParticipante novoPapel) {
        ContextoAtivoService.AcessoContexto acesso = obterAdministrador(principal);
        if (novoPapel == PapelParticipante.DEPENDENTE) {
            throw new RegraNegocioException(
                    HttpStatus.BAD_REQUEST,
                    "papel-dependente-restrito",
                    "O papel DEPENDENTE só pode ser atribuído pelo cadastro de dependente."
            );
        }
        ContextoParticipante participante = obterAtivo(participanteId, acesso.contexto().getId());
        if (participante.getPapel() == PapelParticipante.DEPENDENTE) {
            throw new RegraNegocioException(
                    HttpStatus.CONFLICT,
                    "dependente-gerenciado-separadamente",
                    "Gerencie este acesso pela área de dependentes."
            );
        }
        validarAdministradorRestante(participante, novoPapel == PapelParticipante.ADMINISTRADOR);
        PapelParticipante papelAnterior = participante.getPapel();
        participante.alterarPapel(novoPapel);
        auditoriaService.registrar(
                acesso.usuario(), acesso.contexto(), "ALTERACAO_PAPEL",
                "CONTEXTO_PARTICIPANTE", participante.getId(),
                "Papel alterado de " + papelAnterior + " para " + novoPapel + "."
        );
        return ParticipanteContextoResponse.de(participante);
    }

    @Transactional
    public void remover(UsuarioPrincipal principal, Long participanteId) {
        ContextoAtivoService.AcessoContexto acesso = obterAdministrador(principal);
        ContextoParticipante participante = obterAtivo(participanteId, acesso.contexto().getId());
        if (participante.getUsuario().getId().equals(principal.usuarioId())) {
            throw new RegraNegocioException(
                    HttpStatus.BAD_REQUEST,
                    "use-saida-voluntaria",
                    "Use a opção de sair do contexto para encerrar sua própria participação."
            );
        }
        if (participante.getPapel() == PapelParticipante.DEPENDENTE) {
            throw new RegraNegocioException(
                    HttpStatus.CONFLICT,
                    "dependente-gerenciado-separadamente",
                    "Remova este acesso pela área de dependentes."
            );
        }
        validarAdministradorRestante(participante, false);
        encerrarParticipacao(participante, acesso, "REMOCAO_PARTICIPANTE",
                "Participante removido pelo administrador.");
    }

    @Transactional
    public void sair(UsuarioPrincipal principal) {
        ContextoAtivoService.AcessoContexto acesso = obterCompartilhado(principal);
        ContextoParticipante participante = participanteRepository
                .findByContextoIdAndUsuarioIdAndDataSaidaIsNull(
                        acesso.contexto().getId(), principal.usuarioId()
                ).orElseThrow(this::participanteInvalido);
        validarAdministradorRestante(participante, false);
        Instant agora = Instant.now(clock);
        participante.encerrar(agora);
        dependenteRepository.findByParticipanteIdAndDataExclusaoIsNull(participante.getId())
                .ifPresent(dependente -> dependente.desvincularParticipante(agora));
        sessaoRepository.limparContextoAtivo(principal.usuarioId(), acesso.contexto().getId());
        auditoriaService.registrar(
                acesso.usuario(), acesso.contexto(), "SAIDA_CONTEXTO",
                "CONTEXTO_PARTICIPANTE", participante.getId(),
                "Participante saiu voluntariamente do contexto."
        );
    }

    private void encerrarParticipacao(ContextoParticipante participante,
                                       ContextoAtivoService.AcessoContexto acesso,
                                       String acao, String detalhes) {
        Instant agora = Instant.now(clock);
        participante.encerrar(agora);
        sessaoRepository.limparContextoAtivo(
                participante.getUsuario().getId(), acesso.contexto().getId()
        );
        auditoriaService.registrar(
                acesso.usuario(), acesso.contexto(), acao,
                "CONTEXTO_PARTICIPANTE", participante.getId(), detalhes
        );
    }

    private ContextoAtivoService.AcessoContexto obterCompartilhado(UsuarioPrincipal principal) {
        ContextoAtivoService.AcessoContexto acesso = contextoAtivoService.obterParaLeitura(principal);
        if (acesso.contexto().getTipo() != TipoContexto.COMPARTILHADO) {
            throw new RegraNegocioException(
                    HttpStatus.CONFLICT,
                    "contexto-compartilhado-obrigatorio",
                    "Esta operação está disponível apenas em contextos compartilhados."
            );
        }
        return acesso;
    }

    private ContextoAtivoService.AcessoContexto obterAdministrador(UsuarioPrincipal principal) {
        ContextoAtivoService.AcessoContexto acesso = obterCompartilhado(principal);
        if (acesso.papel() != PapelParticipante.ADMINISTRADOR) {
            throw new RegraNegocioException(
                    HttpStatus.FORBIDDEN,
                    "administrador-obrigatorio",
                    "Apenas um administrador pode gerenciar participantes."
            );
        }
        return acesso;
    }

    private ContextoParticipante obterAtivo(Long participanteId, Long contextoId) {
        ContextoParticipante participante = participanteRepository
                .findByIdAndContextoId(participanteId, contextoId)
                .orElseThrow(this::participanteInvalido);
        if (participante.getDataSaida() != null) {
            throw participanteInvalido();
        }
        return participante;
    }

    private void validarAdministradorRestante(ContextoParticipante participante,
                                               boolean permaneceraAdministrador) {
        if (participante.getPapel() != PapelParticipante.ADMINISTRADOR
                || permaneceraAdministrador) {
            return;
        }
        long administradores = participanteRepository.countByContextoIdAndPapelAndDataSaidaIsNull(
                participante.getContexto().getId(), PapelParticipante.ADMINISTRADOR
        );
        if (administradores <= 1) {
            throw new RegraNegocioException(
                    HttpStatus.CONFLICT,
                    "ultimo-administrador",
                    "Transfira o papel de administrador antes de concluir esta operação."
            );
        }
    }

    private RegraNegocioException participanteInvalido() {
        return new RegraNegocioException(
                HttpStatus.NOT_FOUND,
                "participante-nao-encontrado",
                "O participante informado não foi encontrado."
        );
    }
}
