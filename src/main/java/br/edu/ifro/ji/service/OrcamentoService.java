package br.edu.ifro.ji.service;

import br.edu.ifro.ji.dto.OrcamentoRequest;
import br.edu.ifro.ji.dto.OrcamentoResponse;
import br.edu.ifro.ji.dto.ResumoOrcamentosResponse;
import br.edu.ifro.ji.exception.RegraNegocioException;
import br.edu.ifro.ji.model.Categoria;
import br.edu.ifro.ji.model.OrcamentoCategoria;
import br.edu.ifro.ji.repository.OrcamentoCategoriaRepository;
import br.edu.ifro.ji.repository.TransacaoRepository;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrcamentoService {

    private final OrcamentoCategoriaRepository orcamentoRepository;
    private final TransacaoRepository transacaoRepository;
    private final CategoriaService categoriaService;
    private final ContextoAtivoService contextoAtivoService;
    private final MesReferenciaService mesReferenciaService;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public OrcamentoService(OrcamentoCategoriaRepository orcamentoRepository,
                            TransacaoRepository transacaoRepository,
                            CategoriaService categoriaService,
                            ContextoAtivoService contextoAtivoService,
                            MesReferenciaService mesReferenciaService,
                            AuditoriaService auditoriaService,
                            Clock clock) {
        this.orcamentoRepository = orcamentoRepository;
        this.transacaoRepository = transacaoRepository;
        this.categoriaService = categoriaService;
        this.contextoAtivoService = contextoAtivoService;
        this.mesReferenciaService = mesReferenciaService;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ResumoOrcamentosResponse listar(UsuarioPrincipal principal,
                                           String mesInformado) {
        ContextoAtivoService.AcessoContexto acesso =
                contextoAtivoService.obterParaLeitura(principal);
        YearMonth mes = mesReferenciaService.interpretarOuAtual(mesInformado);
        LocalDate primeiroDia = mes.atDay(1);
        LocalDate ultimoDia = mes.atEndOfMonth();
        Map<Long, BigDecimal> gastos = transacaoRepository
                .resumirSaidasPorCategoria(
                        acesso.contexto().getId(), primeiroDia, ultimoDia)
                .stream()
                .collect(Collectors.toMap(
                        TransacaoRepository.GastoCategoriaProjection::getCategoriaId,
                        TransacaoRepository.GastoCategoriaProjection::getTotal
                ));
        var itens = orcamentoRepository
                .findByContextoIdAndMesReferenciaOrderByCategoriaNomeAsc(
                        acesso.contexto().getId(), primeiroDia)
                .stream()
                .map(orcamento -> OrcamentoResponse.de(
                        orcamento,
                        gastos.getOrDefault(
                                orcamento.getCategoria().getId(), BigDecimal.ZERO)))
                .toList();
        return ResumoOrcamentosResponse.de(mes.toString(), itens);
    }

    @Transactional
    public OrcamentoResponse criar(UsuarioPrincipal principal,
                                    OrcamentoRequest request) {
        ContextoAtivoService.AcessoContexto acesso =
                contextoAtivoService.obterParaEscrita(principal);
        LocalDate mes = mesReferenciaService.primeiroDia(request.mesReferencia());
        validarDuplicidade(
                acesso.contexto().getId(), request.categoriaId(), mes, null);
        Categoria categoria = categoriaService.buscarDisponivel(
                request.categoriaId(), acesso.contexto().getId());
        Instant agora = Instant.now(clock);
        OrcamentoCategoria orcamento = orcamentoRepository.saveAndFlush(
                new OrcamentoCategoria(
                        acesso.contexto(), categoria, request.valorLimite(), mes, agora));
        auditoriaService.registrar(
                acesso.usuario(), acesso.contexto(), "CRIACAO_ORCAMENTO",
                "ORCAMENTO_CATEGORIA", orcamento.getId(),
                "Orçamento mensal por categoria criado."
        );
        return OrcamentoResponse.de(
                orcamento,
                gastoDaCategoria(acesso.contexto().getId(), categoria.getId(), mes));
    }

    @Transactional
    public OrcamentoResponse editar(UsuarioPrincipal principal, Long orcamentoId,
                                     OrcamentoRequest request) {
        ContextoAtivoService.AcessoContexto acesso =
                contextoAtivoService.obterParaEscrita(principal);
        OrcamentoCategoria orcamento = buscarNoContexto(
                orcamentoId, acesso.contexto().getId());
        LocalDate mes = mesReferenciaService.primeiroDia(request.mesReferencia());
        validarDuplicidade(
                acesso.contexto().getId(), request.categoriaId(), mes, orcamentoId);
        Categoria categoria = categoriaService.buscarDisponivel(
                request.categoriaId(), acesso.contexto().getId());
        orcamento.atualizar(
                categoria, request.valorLimite(), mes, Instant.now(clock));
        orcamentoRepository.flush();
        auditoriaService.registrar(
                acesso.usuario(), acesso.contexto(), "ATUALIZACAO_ORCAMENTO",
                "ORCAMENTO_CATEGORIA", orcamentoId,
                "Orçamento mensal por categoria atualizado."
        );
        return OrcamentoResponse.de(
                orcamento,
                gastoDaCategoria(acesso.contexto().getId(), categoria.getId(), mes));
    }

    @Transactional
    public void excluir(UsuarioPrincipal principal, Long orcamentoId) {
        ContextoAtivoService.AcessoContexto acesso =
                contextoAtivoService.obterParaEscrita(principal);
        OrcamentoCategoria orcamento = buscarNoContexto(
                orcamentoId, acesso.contexto().getId());
        orcamentoRepository.delete(orcamento);
        auditoriaService.registrar(
                acesso.usuario(), acesso.contexto(), "EXCLUSAO_ORCAMENTO",
                "ORCAMENTO_CATEGORIA", orcamentoId,
                "Orçamento mensal por categoria removido."
        );
    }

    private BigDecimal gastoDaCategoria(Long contextoId, Long categoriaId,
                                         LocalDate mes) {
        return transacaoRepository
                .resumirSaidasPorCategoria(
                        contextoId, mes, YearMonth.from(mes).atEndOfMonth())
                .stream()
                .filter(gasto -> gasto.getCategoriaId().equals(categoriaId))
                .map(TransacaoRepository.GastoCategoriaProjection::getTotal)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private OrcamentoCategoria buscarNoContexto(Long orcamentoId, Long contextoId) {
        return orcamentoRepository.findByIdAndContextoId(orcamentoId, contextoId)
                .orElseThrow(() -> new RegraNegocioException(
                        HttpStatus.NOT_FOUND,
                        "orcamento-nao-encontrado",
                        "O orçamento não foi encontrado no contexto ativo."
                ));
    }

    private void validarDuplicidade(Long contextoId, Long categoriaId,
                                    LocalDate mes, Long orcamentoIgnoradoId) {
        boolean existe = orcamentoIgnoradoId == null
                ? orcamentoRepository
                        .existsByContextoIdAndCategoriaIdAndMesReferencia(
                                contextoId, categoriaId, mes)
                : orcamentoRepository
                        .existsByContextoIdAndCategoriaIdAndMesReferenciaAndIdNot(
                                contextoId, categoriaId, mes, orcamentoIgnoradoId);
        if (existe) {
            throw new RegraNegocioException(
                    HttpStatus.CONFLICT,
                    "orcamento-duplicado",
                    "Já existe um orçamento para essa categoria no mês informado."
            );
        }
    }
}
