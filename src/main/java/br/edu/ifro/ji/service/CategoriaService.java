package br.edu.ifro.ji.service;

import br.edu.ifro.ji.dto.CategoriaRequest;
import br.edu.ifro.ji.dto.CategoriaResponse;
import br.edu.ifro.ji.exception.RegraNegocioException;
import br.edu.ifro.ji.model.Categoria;
import br.edu.ifro.ji.repository.CategoriaRepository;
import br.edu.ifro.ji.repository.OrcamentoCategoriaRepository;
import br.edu.ifro.ji.repository.TransacaoRepository;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final TransacaoRepository transacaoRepository;
    private final OrcamentoCategoriaRepository orcamentoRepository;
    private final ContextoAtivoService contextoAtivoService;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public CategoriaService(CategoriaRepository categoriaRepository,
                            TransacaoRepository transacaoRepository,
                            OrcamentoCategoriaRepository orcamentoRepository,
                            ContextoAtivoService contextoAtivoService,
                            AuditoriaService auditoriaService,
                            Clock clock) {
        this.categoriaRepository = categoriaRepository;
        this.transacaoRepository = transacaoRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.contextoAtivoService = contextoAtivoService;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(UsuarioPrincipal principal) {
        ContextoAtivoService.AcessoContexto acesso =
                contextoAtivoService.obterParaLeitura(principal);
        return categoriaRepository.listarDisponiveis(acesso.contexto().getId())
                .stream()
                .map(CategoriaResponse::de)
                .toList();
    }

    @Transactional
    public CategoriaResponse criar(UsuarioPrincipal principal, CategoriaRequest request) {
        ContextoAtivoService.AcessoContexto acesso =
                contextoAtivoService.obterParaEscrita(principal);
        String nome = normalizarNome(request.nome());
        validarNomeDisponivel(acesso.contexto().getId(), nome, null);

        Categoria categoria = categoriaRepository.save(
                new Categoria(acesso.contexto(), nome));
        auditoriaService.registrar(
                acesso.usuario(), acesso.contexto(), "CRIACAO_CATEGORIA",
                "CATEGORIA", categoria.getId(), "Categoria personalizada criada."
        );
        return CategoriaResponse.de(categoria);
    }

    @Transactional
    public CategoriaResponse editar(UsuarioPrincipal principal, Long categoriaId,
                                     CategoriaRequest request) {
        ContextoAtivoService.AcessoContexto acesso =
                contextoAtivoService.obterParaEscrita(principal);
        Categoria categoria = buscarPersonalizada(categoriaId, acesso.contexto().getId());
        String nome = normalizarNome(request.nome());
        validarNomeDisponivel(acesso.contexto().getId(), nome, categoriaId);

        categoria.renomear(nome);
        auditoriaService.registrar(
                acesso.usuario(), acesso.contexto(), "ATUALIZACAO_CATEGORIA",
                "CATEGORIA", categoria.getId(), "Categoria personalizada renomeada."
        );
        return CategoriaResponse.de(categoria);
    }

    @Transactional
    public void excluir(UsuarioPrincipal principal, Long categoriaId) {
        ContextoAtivoService.AcessoContexto acesso =
                contextoAtivoService.obterParaEscrita(principal);
        Categoria categoria = buscarPersonalizada(categoriaId, acesso.contexto().getId());
        boolean utilizada = transacaoRepository.existsByCategoriaId(categoriaId)
                || orcamentoRepository.existsByCategoriaId(categoriaId);
        if (utilizada) {
            categoria.excluir(Instant.now(clock));
        } else {
            categoriaRepository.delete(categoria);
        }
        auditoriaService.registrar(
                acesso.usuario(), acesso.contexto(), "EXCLUSAO_CATEGORIA",
                "CATEGORIA", categoriaId,
                utilizada
                        ? "Categoria personalizada arquivada por possuir histórico."
                        : "Categoria personalizada não utilizada removida."
        );
    }

    @Transactional(readOnly = true)
    public Categoria buscarDisponivel(Long categoriaId, Long contextoId) {
        return categoriaRepository.buscarDisponivel(categoriaId, contextoId)
                .orElseThrow(() -> new RegraNegocioException(
                        HttpStatus.BAD_REQUEST,
                        "categoria-indisponivel",
                        "A categoria informada não está disponível no contexto ativo."
                ));
    }

    private Categoria buscarPersonalizada(Long categoriaId, Long contextoId) {
        return categoriaRepository.buscarPersonalizadaAtiva(categoriaId, contextoId)
                .orElseThrow(() -> new RegraNegocioException(
                        HttpStatus.NOT_FOUND,
                        "categoria-nao-encontrada",
                        "A categoria personalizada não foi encontrada no contexto ativo."
                ));
    }

    private void validarNomeDisponivel(Long contextoId, String nome, Long categoriaIgnoradaId) {
        if (categoriaRepository.existeDisponivelComNome(
                contextoId, nome, categoriaIgnoradaId)) {
            throw new RegraNegocioException(
                    HttpStatus.CONFLICT,
                    "categoria-nome-em-uso",
                    "Já existe uma categoria disponível com esse nome."
            );
        }
    }

    private String normalizarNome(String nome) {
        return nome.trim().replaceAll("\\s+", " ");
    }
}
