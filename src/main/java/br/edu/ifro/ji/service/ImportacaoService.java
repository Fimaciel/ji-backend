package br.edu.ifro.ji.service;

import br.edu.ifro.ji.dto.CategoriaResponse;
import br.edu.ifro.ji.dto.ImportacaoAnaliseResponse;
import br.edu.ifro.ji.dto.ImportacaoConfirmacaoRequest;
import br.edu.ifro.ji.dto.ImportacaoRegistroRequest;
import br.edu.ifro.ji.dto.ImportacaoRegistroResponse;
import br.edu.ifro.ji.dto.ImportacaoResponse;
import br.edu.ifro.ji.exception.RegraNegocioException;
import br.edu.ifro.ji.model.Categoria;
import br.edu.ifro.ji.model.ImportacaoArquivo;
import br.edu.ifro.ji.model.StatusImportacao;
import br.edu.ifro.ji.model.TipoTransacao;
import br.edu.ifro.ji.model.Transacao;
import br.edu.ifro.ji.repository.CategoriaRepository;
import br.edu.ifro.ji.repository.ImportacaoArquivoRepository;
import br.edu.ifro.ji.repository.TransacaoRepository;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ImportacaoService {

    private final LeitorImportacaoService leitor;
    private final ImportacaoArquivoRepository importacaoRepository;
    private final TransacaoRepository transacaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ContextoAtivoService contextoAtivoService;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public ImportacaoService(LeitorImportacaoService leitor,
                             ImportacaoArquivoRepository importacaoRepository,
                             TransacaoRepository transacaoRepository,
                             CategoriaRepository categoriaRepository,
                             ContextoAtivoService contextoAtivoService,
                             AuditoriaService auditoriaService,
                             Clock clock) {
        this.leitor = leitor;
        this.importacaoRepository = importacaoRepository;
        this.transacaoRepository = transacaoRepository;
        this.categoriaRepository = categoriaRepository;
        this.contextoAtivoService = contextoAtivoService;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ImportacaoAnaliseResponse analisar(UsuarioPrincipal principal, MultipartFile arquivo) {
        ContextoAtivoService.AcessoContexto acesso =
                contextoAtivoService.obterParaEscrita(principal);
        LeitorImportacaoService.ArquivoLido lido = leitor.ler(arquivo);
        List<Categoria> categorias = categoriaRepository.listarDisponiveis(
                acesso.contexto().getId());
        Map<String, Categoria> categoriasPorNome = categoriasPorNome(categorias);
        Set<ChaveTransacao> existentes = chavesExistentes(
                acesso.contexto().getId(), lido.registros());
        Set<ChaveTransacao> vistas = new HashSet<>();
        List<ImportacaoRegistroResponse> registros = new ArrayList<>();
        int duplicados = 0;
        for (LeitorImportacaoService.RegistroLido registro : lido.registros()) {
            ChaveTransacao chave = chave(registro.data(), registro.valor(), registro.descricao());
            boolean duplicado = existentes.contains(chave) || !vistas.add(chave);
            if (duplicado) {
                duplicados++;
            }
            Categoria categoria = sugerirCategoria(registro, categoriasPorNome);
            registros.add(new ImportacaoRegistroResponse(
                    registro.linha(), registro.data(), registro.descricao(), registro.valor(),
                    registro.tipo(), CategoriaResponse.de(categoria), duplicado
            ));
        }
        return new ImportacaoAnaliseResponse(
                lido.nome(), lido.formato(), registros.size(),
                registros.size() - duplicados, duplicados, registros
        );
    }

    @Transactional
    public ImportacaoResponse confirmar(UsuarioPrincipal principal,
                                         ImportacaoConfirmacaoRequest request) {
        ContextoAtivoService.AcessoContexto acesso =
                contextoAtivoService.obterParaEscrita(principal);
        validarResumo(request);

        Map<Long, Categoria> categorias = new HashMap<>();
        categoriaRepository.listarDisponiveis(acesso.contexto().getId())
                .forEach(categoria -> categorias.put(categoria.getId(), categoria));
        List<ImportacaoRegistroRequest> candidatos = request.registros().stream()
                .map(this::normalizar)
                .toList();
        Set<ChaveTransacao> existentes = chavesExistentesConfirmacao(
                acesso.contexto().getId(), candidatos);
        Set<ChaveTransacao> vistas = new LinkedHashSet<>();
        List<RegistroConfirmado> novos = new ArrayList<>();
        int duplicadosNaConfirmacao = 0;
        for (ImportacaoRegistroRequest registro : candidatos) {
            Categoria categoria = categorias.get(registro.categoriaId());
            if (categoria == null) {
                throw new RegraNegocioException(
                        HttpStatus.BAD_REQUEST,
                        "categoria-indisponivel",
                        "Uma das categorias selecionadas não está disponível no contexto ativo."
                );
            }
            ChaveTransacao chave = chave(registro.data(), registro.valor(), registro.descricao());
            if (existentes.contains(chave) || !vistas.add(chave)) {
                duplicadosNaConfirmacao++;
                continue;
            }
            novos.add(new RegistroConfirmado(registro, categoria));
        }
        if (novos.isEmpty()) {
            throw new RegraNegocioException(
                    HttpStatus.CONFLICT,
                    "importacao-sem-registros-novos",
                    "Todos os registros selecionados já existem no contexto ativo."
            );
        }

        Instant agora = Instant.now(clock);
        int totalDuplicados = request.totalDuplicados() + duplicadosNaConfirmacao;
        ImportacaoArquivo importacao = importacaoRepository.save(new ImportacaoArquivo(
                acesso.contexto(), acesso.usuario(), normalizarNomeArquivo(request.nomeArquivo()),
                request.formato(), request.totalLidos(), novos.size(), totalDuplicados, agora
        ));
        List<Transacao> transacoes = novos.stream().map(novo -> {
            ImportacaoRegistroRequest registro = novo.registro();
            Transacao transacao = new Transacao(
                    acesso.contexto(), acesso.usuario(), novo.categoria(), registro.tipo(),
                    registro.valor(), registro.data(), registro.descricao(), agora
            );
            transacao.vincularImportacao(importacao);
            return transacao;
        }).toList();
        transacaoRepository.saveAll(transacoes);
        auditoriaService.registrar(
                acesso.usuario(), acesso.contexto(), "IMPORTACAO_TRANSACOES",
                "IMPORTACAO_ARQUIVO", importacao.getId(),
                detalhes("Arquivo %s: %d registro(s) importado(s) e %d duplicado(s) ignorado(s)."
                        .formatted(importacao.getNomeArquivo(), novos.size(), totalDuplicados))
        );
        return ImportacaoResponse.de(importacao);
    }

    @Transactional(readOnly = true)
    public List<ImportacaoResponse> listar(UsuarioPrincipal principal) {
        ContextoAtivoService.AcessoContexto acesso =
                contextoAtivoService.obterParaLeitura(principal);
        return importacaoRepository
                .findTop20ByContextoIdOrderByDataCriacaoDescIdDesc(acesso.contexto().getId())
                .stream()
                .map(ImportacaoResponse::de)
                .toList();
    }

    @Transactional
    public ImportacaoResponse desfazerUltima(UsuarioPrincipal principal) {
        ContextoAtivoService.AcessoContexto acesso =
                contextoAtivoService.obterParaEscrita(principal);
        ImportacaoArquivo importacao = importacaoRepository
                .findFirstByContextoIdAndStatusOrderByDataCriacaoDescIdDesc(
                        acesso.contexto().getId(), StatusImportacao.CONCLUIDA)
                .orElseThrow(() -> new RegraNegocioException(
                        HttpStatus.NOT_FOUND,
                        "importacao-nao-encontrada",
                        "Não há uma importação concluída para desfazer neste contexto."
                ));
        int removidas = transacaoRepository.excluirPorImportacao(importacao.getId());
        importacao.desfazer(Instant.now(clock));
        auditoriaService.registrar(
                acesso.usuario(), acesso.contexto(), "DESFAZIMENTO_IMPORTACAO",
                "IMPORTACAO_ARQUIVO", importacao.getId(),
                detalhes("Importação %s desfeita; %d transação(ões) removida(s)."
                        .formatted(importacao.getNomeArquivo(), removidas))
        );
        return ImportacaoResponse.de(importacao);
    }

    private void validarResumo(ImportacaoConfirmacaoRequest request) {
        if (request.totalLidos() < 0 || request.totalDuplicados() < 0
                || request.totalLidos() < request.registros().size() + request.totalDuplicados()) {
            throw new RegraNegocioException(
                    HttpStatus.BAD_REQUEST,
                    "resumo-importacao-invalido",
                    "O resumo da importação não corresponde aos registros selecionados."
            );
        }
    }

    private ImportacaoRegistroRequest normalizar(ImportacaoRegistroRequest registro) {
        return new ImportacaoRegistroRequest(
                registro.data(), normalizarDescricao(registro.descricao()),
                registro.valor().abs().setScale(2, java.math.RoundingMode.HALF_UP),
                registro.tipo(), registro.categoriaId()
        );
    }

    private Set<ChaveTransacao> chavesExistentes(
            Long contextoId, List<LeitorImportacaoService.RegistroLido> registros) {
        var inicio = registros.stream().map(LeitorImportacaoService.RegistroLido::data)
                .min(java.time.LocalDate::compareTo).orElseThrow();
        var fim = registros.stream().map(LeitorImportacaoService.RegistroLido::data)
                .max(java.time.LocalDate::compareTo).orElseThrow();
        return transacaoRepository.findByContextoIdAndDataBetweenOrderByDataAscIdAsc(
                        contextoId, inicio, fim).stream()
                .map(transacao -> chave(
                        transacao.getData(), transacao.getValor(), transacao.getDescricao()))
                .collect(java.util.stream.Collectors.toSet());
    }

    private Set<ChaveTransacao> chavesExistentesConfirmacao(
            Long contextoId, List<ImportacaoRegistroRequest> registros) {
        var inicio = registros.stream().map(ImportacaoRegistroRequest::data)
                .min(java.time.LocalDate::compareTo).orElseThrow();
        var fim = registros.stream().map(ImportacaoRegistroRequest::data)
                .max(java.time.LocalDate::compareTo).orElseThrow();
        return transacaoRepository.findByContextoIdAndDataBetweenOrderByDataAscIdAsc(
                        contextoId, inicio, fim).stream()
                .map(transacao -> chave(
                        transacao.getData(), transacao.getValor(), transacao.getDescricao()))
                .collect(java.util.stream.Collectors.toSet());
    }

    private Map<String, Categoria> categoriasPorNome(List<Categoria> categorias) {
        Map<String, Categoria> resultado = new HashMap<>();
        categorias.forEach(categoria -> resultado.put(
                normalizarTexto(categoria.getNome()), categoria));
        return resultado;
    }

    private Categoria sugerirCategoria(LeitorImportacaoService.RegistroLido registro,
                                       Map<String, Categoria> categorias) {
        String descricao = normalizarTexto(registro.descricao());
        String nome;
        if (registro.tipo() == TipoTransacao.ENTRADA) {
            nome = "salario";
        } else if (contem(descricao, "mercado", "supermercado", "restaurante", "padaria", "alimento")) {
            nome = "alimentacao";
        } else if (contem(descricao, "uber", "taxi", "combust", "onibus", "passagem", "transporte")) {
            nome = "transporte";
        } else if (contem(descricao, "farmacia", "medic", "clinica", "hospital", "saude")) {
            nome = "saude";
        } else if (contem(descricao, "cinema", "spotify", "netflix", "lazer", "show")) {
            nome = "lazer";
        } else {
            nome = "outros";
        }
        Categoria categoria = categorias.get(nome);
        if (categoria == null) {
            throw new IllegalStateException("Categoria padrão indisponível: " + nome);
        }
        return categoria;
    }

    private boolean contem(String texto, String... termos) {
        for (String termo : termos) {
            if (texto.contains(termo)) {
                return true;
            }
        }
        return false;
    }

    private ChaveTransacao chave(java.time.LocalDate data, BigDecimal valor, String descricao) {
        return new ChaveTransacao(
                data,
                valor.abs().setScale(2, java.math.RoundingMode.HALF_UP),
                normalizarTexto(descricao)
        );
    }

    private String normalizarNomeArquivo(String nome) {
        String seguro = nome.replace('\\', '/');
        seguro = seguro.substring(seguro.lastIndexOf('/') + 1).trim();
        if (seguro.isBlank() || seguro.length() > 255) {
            throw new RegraNegocioException(
                    HttpStatus.BAD_REQUEST, "nome-arquivo-invalido", "O nome do arquivo é inválido."
            );
        }
        return seguro;
    }

    private String normalizarDescricao(String descricao) {
        return descricao.trim().replaceAll("\\s+", " ");
    }

    private String normalizarTexto(String texto) {
        return Normalizer.normalize(normalizarDescricao(texto), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private String detalhes(String detalhes) {
        return detalhes.length() <= 255 ? detalhes : detalhes.substring(0, 255);
    }

    private record ChaveTransacao(
            java.time.LocalDate data,
            BigDecimal valor,
            String descricao
    ) {
    }

    private record RegistroConfirmado(
            ImportacaoRegistroRequest registro,
            Categoria categoria
    ) {
    }
}
