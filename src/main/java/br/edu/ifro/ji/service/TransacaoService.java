package br.edu.ifro.ji.service;

import br.edu.ifro.ji.dto.PaginaResponse;
import br.edu.ifro.ji.dto.TransacaoRequest;
import br.edu.ifro.ji.dto.TransacaoResponse;
import br.edu.ifro.ji.exception.RegraNegocioException;
import br.edu.ifro.ji.model.Categoria;
import br.edu.ifro.ji.model.TipoTransacao;
import br.edu.ifro.ji.model.Transacao;
import br.edu.ifro.ji.repository.TransacaoRepository;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

@Service
public class TransacaoService {

    private static final int TAMANHO_PAGINA = 20;

    private final TransacaoRepository transacaoRepository;
    private final CategoriaService categoriaService;
    private final ContextoAtivoService contextoAtivoService;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public TransacaoService(TransacaoRepository transacaoRepository,
                            CategoriaService categoriaService,
                            ContextoAtivoService contextoAtivoService,
                            AuditoriaService auditoriaService,
                            Clock clock) {
        this.transacaoRepository = transacaoRepository;
        this.categoriaService = categoriaService;
        this.contextoAtivoService = contextoAtivoService;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<TransacaoResponse> listar(
            UsuarioPrincipal principal,
            int pagina,
            LocalDate dataInicial,
            LocalDate dataFinal,
            TipoTransacao tipo,
            Long categoriaId,
            String busca) {
        ContextoAtivoService.AcessoContexto acesso =
                contextoAtivoService.obterParaLeitura(principal);
        validarPeriodo(dataInicial, dataFinal);
        if (categoriaId != null) {
            categoriaService.buscarDisponivel(categoriaId, acesso.contexto().getId());
        }

        Specification<Transacao> filtro = porContexto(acesso.contexto().getId());
        if (dataInicial != null) {
            filtro = filtro.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("data"), dataInicial));
        }
        if (dataFinal != null) {
            filtro = filtro.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("data"), dataFinal));
        }
        if (tipo != null) {
            filtro = filtro.and((root, query, cb) -> cb.equal(root.get("tipo"), tipo));
        }
        if (categoriaId != null) {
            filtro = filtro.and((root, query, cb) ->
                    cb.equal(root.get("categoria").get("id"), categoriaId));
        }
        if (busca != null && !busca.isBlank()) {
            String trecho = "%" + busca.trim().toLowerCase(Locale.ROOT) + "%";
            filtro = filtro.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("descricao")), trecho),
                    cb.like(cb.lower(root.get("categoria").get("nome")), trecho)
            ));
        }

        Page<TransacaoResponse> resultado = transacaoRepository.findAll(
                        filtro,
                        PageRequest.of(
                                pagina,
                                TAMANHO_PAGINA,
                                Sort.by(Sort.Order.desc("data"), Sort.Order.desc("id"))
                        )
                )
                .map(TransacaoResponse::de);
        return PaginaResponse.de(resultado);
    }

    @Transactional
    public TransacaoResponse criar(UsuarioPrincipal principal, TransacaoRequest request) {
        ContextoAtivoService.AcessoContexto acesso =
                contextoAtivoService.obterParaEscrita(principal);
        validarData(request.data());
        Categoria categoria = categoriaService.buscarDisponivel(
                request.categoriaId(), acesso.contexto().getId());
        Instant agora = Instant.now(clock);
        Transacao transacao = transacaoRepository.save(new Transacao(
                acesso.contexto(),
                acesso.usuario(),
                categoria,
                request.tipo(),
                request.valor(),
                request.data(),
                normalizarDescricao(request.descricao()),
                agora
        ));
        return TransacaoResponse.de(transacao);
    }

    @Transactional
    public TransacaoResponse editar(UsuarioPrincipal principal, Long transacaoId,
                                    TransacaoRequest request) {
        ContextoAtivoService.AcessoContexto acesso =
                contextoAtivoService.obterParaEscrita(principal);
        validarData(request.data());
        Transacao transacao = buscarNoContexto(transacaoId, acesso.contexto().getId());
        Categoria categoria = categoriaService.buscarDisponivel(
                request.categoriaId(), acesso.contexto().getId());
        transacao.atualizar(
                categoria,
                request.tipo(),
                request.valor(),
                request.data(),
                normalizarDescricao(request.descricao()),
                Instant.now(clock)
        );
        return TransacaoResponse.de(transacao);
    }

    @Transactional
    public void excluir(UsuarioPrincipal principal, Long transacaoId) {
        ContextoAtivoService.AcessoContexto acesso =
                contextoAtivoService.obterParaEscrita(principal);
        Transacao transacao = buscarNoContexto(transacaoId, acesso.contexto().getId());
        transacaoRepository.delete(transacao);
        auditoriaService.registrar(
                acesso.usuario(), acesso.contexto(), "EXCLUSAO_TRANSACAO",
                "TRANSACAO", transacaoId, "Transação removida pelo usuário."
        );
    }

    private Specification<Transacao> porContexto(Long contextoId) {
        return (root, query, cb) -> cb.equal(root.get("contexto").get("id"), contextoId);
    }

    private Transacao buscarNoContexto(Long transacaoId, Long contextoId) {
        return transacaoRepository.findByIdAndContextoId(transacaoId, contextoId)
                .orElseThrow(() -> new RegraNegocioException(
                        HttpStatus.NOT_FOUND,
                        "transacao-nao-encontrada",
                        "A transação não foi encontrada no contexto ativo."
                ));
    }

    private void validarPeriodo(LocalDate dataInicial, LocalDate dataFinal) {
        if (dataInicial != null && dataFinal != null && dataInicial.isAfter(dataFinal)) {
            throw new RegraNegocioException(
                    HttpStatus.BAD_REQUEST,
                    "periodo-invalido",
                    "A data inicial não pode ser posterior à data final."
            );
        }
    }

    private void validarData(LocalDate data) {
        if (data.isAfter(LocalDate.now(clock))) {
            throw new RegraNegocioException(
                    HttpStatus.BAD_REQUEST,
                    "data-futura",
                    "A data da transação não pode ser futura."
            );
        }
    }

    private String normalizarDescricao(String descricao) {
        return descricao.trim().replaceAll("\\s+", " ");
    }
}
