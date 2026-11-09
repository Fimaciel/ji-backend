package br.edu.ifro.ji.service;

import br.edu.ifro.ji.dto.DependenteRequest;
import br.edu.ifro.ji.dto.DependenteResponse;
import br.edu.ifro.ji.exception.RegraNegocioException;
import br.edu.ifro.ji.model.ConviteContexto;
import br.edu.ifro.ji.model.Dependente;
import br.edu.ifro.ji.model.PapelParticipante;
import br.edu.ifro.ji.model.StatusConvite;
import br.edu.ifro.ji.model.TipoDependente;
import br.edu.ifro.ji.model.TipoContexto;
import br.edu.ifro.ji.repository.ConviteContextoRepository;
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
public class DependenteService {

    private final ContextoAtivoService contextoAtivoService;
    private final DependenteRepository dependenteRepository;
    private final ConviteContextoRepository conviteRepository;
    private final SessaoRepository sessaoRepository;
    private final GestaoConviteService conviteService;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public DependenteService(ContextoAtivoService contextoAtivoService,
                             DependenteRepository dependenteRepository,
                             ConviteContextoRepository conviteRepository,
                             SessaoRepository sessaoRepository,
                             GestaoConviteService conviteService,
                             AuditoriaService auditoriaService,
                             Clock clock) {
        this.contextoAtivoService = contextoAtivoService;
        this.dependenteRepository = dependenteRepository;
        this.conviteRepository = conviteRepository;
        this.sessaoRepository = sessaoRepository;
        this.conviteService = conviteService;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<DependenteResponse> listar(UsuarioPrincipal principal) {
        ContextoAtivoService.AcessoContexto acesso = obterAdministrador(principal);
        return dependenteRepository
                .findByContextoIdAndDataExclusaoIsNullOrderByNomeAsc(acesso.contexto().getId())
                .stream().map(DependenteResponse::de).toList();
    }

    @Transactional
    public DependenteResponse criar(UsuarioPrincipal principal, DependenteRequest request) {
        ContextoAtivoService.AcessoContexto acesso = obterAdministrador(principal);
        validarEmailLeitura(request, true);
        Instant agora = Instant.now(clock);
        Dependente dependente = dependenteRepository.save(new Dependente(
                acesso.contexto(), request.nome().trim(), request.dataNascimento(), request.tipo(), agora
        ));
        if (request.tipo() == TipoDependente.LEITURA) {
            conviteService.criarParaDependente(
                    acesso.contexto(), acesso.usuario(), request.email(), dependente
            );
        }
        auditoriaService.registrar(
                acesso.usuario(), acesso.contexto(), "CRIACAO_DEPENDENTE",
                "DEPENDENTE", dependente.getId(), "Dependente cadastrado como " + request.tipo() + "."
        );
        return DependenteResponse.de(dependente);
    }

    @Transactional
    public DependenteResponse atualizar(UsuarioPrincipal principal, Long dependenteId,
                                         DependenteRequest request) {
        ContextoAtivoService.AcessoContexto acesso = obterAdministrador(principal);
        Dependente dependente = obter(dependenteId, acesso.contexto().getId());
        validarEmailLeitura(request, dependente.getParticipante() == null);
        if (dependente.getParticipante() != null && request.tipo() != TipoDependente.LEITURA) {
            throw new RegraNegocioException(
                    HttpStatus.CONFLICT,
                    "dependente-com-acesso",
                    "Remova o acesso do dependente antes de alterar seu tipo."
            );
        }

        Instant agora = Instant.now(clock);
        cancelarPendentes(dependente, agora);
        dependente.atualizar(request.nome().trim(), request.dataNascimento(), request.tipo(), agora);
        if (request.tipo() == TipoDependente.LEITURA && dependente.getParticipante() == null) {
            conviteService.criarParaDependente(
                    acesso.contexto(), acesso.usuario(), request.email(), dependente
            );
        }
        auditoriaService.registrar(
                acesso.usuario(), acesso.contexto(), "ATUALIZACAO_DEPENDENTE",
                "DEPENDENTE", dependente.getId(), "Dados do dependente atualizados."
        );
        return DependenteResponse.de(dependente);
    }

    @Transactional
    public void excluir(UsuarioPrincipal principal, Long dependenteId) {
        ContextoAtivoService.AcessoContexto acesso = obterAdministrador(principal);
        Dependente dependente = obter(dependenteId, acesso.contexto().getId());
        Instant agora = Instant.now(clock);
        cancelarPendentes(dependente, agora);
        if (dependente.getParticipante() != null
                && dependente.getParticipante().getDataSaida() == null) {
            dependente.getParticipante().encerrar(agora);
            sessaoRepository.limparContextoAtivo(
                    dependente.getParticipante().getUsuario().getId(), acesso.contexto().getId()
            );
        }
        dependente.desvincularParticipante(agora);
        dependente.excluir(agora);
        dependenteRepository.save(dependente);
        auditoriaService.registrar(
                acesso.usuario(), acesso.contexto(), "EXCLUSAO_DEPENDENTE",
                "DEPENDENTE", dependente.getId(), "Dependente removido do contexto."
        );
    }

    private ContextoAtivoService.AcessoContexto obterAdministrador(UsuarioPrincipal principal) {
        ContextoAtivoService.AcessoContexto acesso = contextoAtivoService.obterParaLeitura(principal);
        if (acesso.contexto().getTipo() != TipoContexto.COMPARTILHADO
                || acesso.papel() != PapelParticipante.ADMINISTRADOR) {
            throw new RegraNegocioException(
                    HttpStatus.FORBIDDEN,
                    "administrador-obrigatorio",
                    "Apenas o administrador de um contexto compartilhado pode gerenciar dependentes."
            );
        }
        return acesso;
    }

    private Dependente obter(Long dependenteId, Long contextoId) {
        return dependenteRepository.findByIdAndContextoIdAndDataExclusaoIsNull(dependenteId, contextoId)
                .orElseThrow(() -> new RegraNegocioException(
                        HttpStatus.NOT_FOUND,
                        "dependente-nao-encontrado",
                        "O dependente informado não foi encontrado."
                ));
    }

    private void validarEmailLeitura(DependenteRequest request, boolean geraraConvite) {
        if (request.tipo() == TipoDependente.LEITURA && geraraConvite
                && (request.email() == null || request.email().isBlank())) {
            throw new RegraNegocioException(
                    HttpStatus.BAD_REQUEST,
                    "email-dependente-obrigatorio",
                    "Informe o e-mail da conta que receberá o acesso de leitura."
            );
        }
    }

    private void cancelarPendentes(Dependente dependente, Instant agora) {
        List<ConviteContexto> pendentes = conviteRepository.findByDependenteIdAndStatus(
                dependente.getId(), StatusConvite.PENDENTE
        );
        pendentes.forEach(convite -> convite.cancelar(agora));
        if (!pendentes.isEmpty()) {
            conviteRepository.flush();
        }
    }
}
