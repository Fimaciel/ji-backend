package br.edu.ifro.ji.service;

import br.edu.ifro.ji.dto.RelatorioCategoriaResponse;
import br.edu.ifro.ji.dto.RelatorioFluxoResponse;
import br.edu.ifro.ji.dto.RelatorioResumoResponse;
import br.edu.ifro.ji.dto.RelatorioTransacaoResponse;
import br.edu.ifro.ji.exception.RegraNegocioException;
import br.edu.ifro.ji.model.ContextoParticipante;
import br.edu.ifro.ji.model.FormatoRelatorio;
import br.edu.ifro.ji.model.TipoTransacao;
import br.edu.ifro.ji.model.Transacao;
import br.edu.ifro.ji.repository.ContextoParticipanteRepository;
import br.edu.ifro.ji.repository.TransacaoRepository;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RelatorioService {

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final TransacaoRepository transacaoRepository;
    private final ContextoParticipanteRepository participanteRepository;
    private final ContextoAtivoService contextoAtivoService;

    public RelatorioService(TransacaoRepository transacaoRepository,
                            ContextoParticipanteRepository participanteRepository,
                            ContextoAtivoService contextoAtivoService) {
        this.transacaoRepository = transacaoRepository;
        this.participanteRepository = participanteRepository;
        this.contextoAtivoService = contextoAtivoService;
    }

    @Transactional(readOnly = true)
    public RelatorioResumoResponse resumir(UsuarioPrincipal principal,
                                            LocalDate dataInicial,
                                            LocalDate dataFinal,
                                            TipoTransacao tipo,
                                            Long categoriaId,
                                            List<Long> contextoIds) {
        return carregar(principal, dataInicial, dataFinal, tipo, categoriaId, contextoIds).resumo();
    }

    @Transactional(readOnly = true)
    public ArquivoRelatorio exportar(UsuarioPrincipal principal,
                                     LocalDate dataInicial,
                                     LocalDate dataFinal,
                                     TipoTransacao tipo,
                                     Long categoriaId,
                                     List<Long> contextoIds,
                                     FormatoRelatorio formato) {
        DadosRelatorio dados = carregar(
                principal, dataInicial, dataFinal, tipo, categoriaId, contextoIds);
        if (dados.transacoes().isEmpty()) {
            throw new RegraNegocioException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "relatorio-sem-dados",
                    "Não há transações no período e nos contextos selecionados."
            );
        }
        String base = "relatorio-ji-" + dataInicial + "-a-" + dataFinal;
        try {
            return formato == FormatoRelatorio.PDF
                    ? new ArquivoRelatorio(
                            gerarPdf(dados), MediaType.APPLICATION_PDF, base + ".pdf")
                    : new ArquivoRelatorio(
                            gerarXlsx(dados),
                            MediaType.parseMediaType(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                            base + ".xlsx");
        } catch (IOException exception) {
            throw new RegraNegocioException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "falha-geracao-relatorio",
                    "Não foi possível gerar o relatório solicitado."
            );
        }
    }

    private DadosRelatorio carregar(UsuarioPrincipal principal,
                                     LocalDate dataInicial,
                                     LocalDate dataFinal,
                                     TipoTransacao tipo,
                                     Long categoriaId,
                                     List<Long> contextoIds) {
        validarPeriodo(dataInicial, dataFinal);
        List<ContextoParticipante> participacoes = participanteRepository
                .findByUsuarioIdAndDataSaidaIsNullOrderByDataIngressoAsc(principal.usuarioId());
        Map<Long, ContextoParticipante> porContexto = new LinkedHashMap<>();
        participacoes.forEach(participacao -> porContexto.put(
                participacao.getContexto().getId(), participacao));

        Set<Long> selecionados = new LinkedHashSet<>();
        if (contextoIds == null || contextoIds.isEmpty()) {
            selecionados.add(contextoAtivoService.obterParaLeitura(principal).contexto().getId());
        } else {
            selecionados.addAll(contextoIds);
        }
        if (selecionados.isEmpty() || selecionados.size() > 20
                || !porContexto.keySet().containsAll(selecionados)) {
            throw new RegraNegocioException(
                    HttpStatus.FORBIDDEN,
                    "contexto-relatorio-sem-acesso",
                    "Um dos contextos selecionados não está disponível para sua conta."
            );
        }

        Specification<Transacao> filtro = (root, query, cb) ->
                root.get("contexto").get("id").in(selecionados);
        filtro = filtro.and((root, query, cb) ->
                cb.between(root.get("data"), dataInicial, dataFinal));
        if (tipo != null) {
            filtro = filtro.and((root, query, cb) -> cb.equal(root.get("tipo"), tipo));
        }
        if (categoriaId != null) {
            filtro = filtro.and((root, query, cb) ->
                    cb.equal(root.get("categoria").get("id"), categoriaId));
        }
        List<Transacao> transacoes = transacaoRepository.findAll(
                filtro, Sort.by(Sort.Order.asc("data"), Sort.Order.asc("id")));
        List<String> nomesContextos = selecionados.stream()
                .map(id -> porContexto.get(id).getContexto().getNome())
                .toList();
        RelatorioResumoResponse resumo = montarResumo(
                dataInicial, dataFinal, tipo, nomesContextos, transacoes);
        return new DadosRelatorio(resumo, transacoes);
    }

    private RelatorioResumoResponse montarResumo(LocalDate dataInicial,
                                                  LocalDate dataFinal,
                                                  TipoTransacao tipo,
                                                  List<String> contextos,
                                                  List<Transacao> transacoes) {
        BigDecimal entradas = transacoes.stream()
                .filter(transacao -> transacao.getTipo() == TipoTransacao.ENTRADA)
                .map(Transacao::getValor)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal saidas = transacoes.stream()
                .filter(transacao -> transacao.getTipo() == TipoTransacao.SAIDA)
                .map(Transacao::getValor)
                .reduce(ZERO, BigDecimal::add);

        Map<YearMonth, TotaisPeriodo> fluxo = new LinkedHashMap<>();
        Map<String, TotaisCategoria> categorias = new LinkedHashMap<>();
        List<RelatorioTransacaoResponse> itens = new ArrayList<>();
        for (Transacao transacao : transacoes) {
            TotaisPeriodo periodo = fluxo.computeIfAbsent(
                    YearMonth.from(transacao.getData()), ignorado -> new TotaisPeriodo());
            if (transacao.getTipo() == TipoTransacao.ENTRADA) {
                periodo.entradas = periodo.entradas.add(transacao.getValor());
            } else {
                periodo.saidas = periodo.saidas.add(transacao.getValor());
            }
            TotaisCategoria categoria = categorias.computeIfAbsent(
                    transacao.getCategoria().getNome(), ignorado -> new TotaisCategoria());
            categoria.quantidade++;
            categoria.total = categoria.total.add(transacao.getValor());
            itens.add(new RelatorioTransacaoResponse(
                    transacao.getId(), transacao.getContexto().getNome(), transacao.getData(),
                    transacao.getDescricao(), transacao.getCategoria().getNome(),
                    transacao.getTipo(), transacao.getValor()
            ));
        }

        List<RelatorioFluxoResponse> fluxoMensal = fluxo.entrySet().stream()
                .map(item -> new RelatorioFluxoResponse(
                        item.getKey(), item.getValue().entradas, item.getValue().saidas))
                .toList();
        List<RelatorioCategoriaResponse> resumoCategorias = categorias.entrySet().stream()
                .map(item -> new RelatorioCategoriaResponse(
                        item.getKey(), item.getValue().quantidade, item.getValue().total,
                        item.getValue().total.divide(
                                BigDecimal.valueOf(item.getValue().quantidade),
                                2, RoundingMode.HALF_UP)))
                .sorted((a, b) -> b.total().compareTo(a.total()))
                .toList();
        return new RelatorioResumoResponse(
                dataInicial, dataFinal, tipo, contextos, entradas, saidas,
                entradas.subtract(saidas), transacoes.size(), fluxoMensal,
                resumoCategorias, itens
        );
    }

    private byte[] gerarPdf(DadosRelatorio dados) throws IOException {
        try (PDDocument documento = new PDDocument();
             ByteArrayOutputStream saida = new ByteArrayOutputStream();
             EscritorPdf escritor = new EscritorPdf(documento)) {
            RelatorioResumoResponse resumo = dados.resumo();
            escritor.titulo("Ji - Relatorio financeiro");
            escritor.texto("Periodo: " + DATA_BR.format(resumo.dataInicial()) + " a "
                    + DATA_BR.format(resumo.dataFinal()), 10);
            escritor.texto("Contextos: " + String.join(", ", resumo.contextos()), 10);
            escritor.texto("Tipo: " + (resumo.tipo() == null ? "Geral" : resumo.tipo()), 10);
            escritor.espaco(8);
            escritor.texto("Entradas: " + moeda(resumo.totalEntradas())
                    + "   Saidas: " + moeda(resumo.totalSaidas())
                    + "   Saldo: " + moeda(resumo.saldo()), 11);
            escritor.espaco(14);
            escritor.texto("Data       Contexto             Descricao                    Tipo     Valor", 9);
            escritor.linha();
            for (RelatorioTransacaoResponse transacao : resumo.transacoes()) {
                String linha = "%s  %-20s %-28s %-8s %12s".formatted(
                        DATA_BR.format(transacao.data()),
                        limitar(transacao.contexto(), 20),
                        limitar(transacao.descricao(), 28),
                        transacao.tipo(),
                        moeda(transacao.valor())
                );
                escritor.texto(linha, 8);
            }
            escritor.espaco(10);
            escritor.texto("Total de transacoes: " + resumo.quantidadeTransacoes(), 9);
            escritor.fecharConteudo();
            documento.save(saida);
            return saida.toByteArray();
        }
    }

    private byte[] gerarXlsx(DadosRelatorio dados) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            RelatorioResumoResponse resumo = dados.resumo();
            Sheet planilha = workbook.createSheet("Relatório");
            CellStyle titulo = estiloTitulo(workbook);
            CellStyle cabecalho = estiloCabecalho(workbook);
            CellStyle moeda = estiloMoeda(workbook);
            CellStyle data = estiloData(workbook);

            Row linha = planilha.createRow(0);
            linha.createCell(0).setCellValue("Ji - Relatório financeiro");
            linha.getCell(0).setCellStyle(titulo);
            planilha.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 6));
            planilha.createRow(2).createCell(0).setCellValue(
                    "Período: " + DATA_BR.format(resumo.dataInicial()) + " a "
                            + DATA_BR.format(resumo.dataFinal()));
            planilha.createRow(3).createCell(0).setCellValue(
                    "Contextos: " + String.join(", ", resumo.contextos()));

            Row totais = planilha.createRow(5);
            totais.createCell(0).setCellValue("Entradas");
            totais.createCell(1).setCellValue(resumo.totalEntradas().doubleValue());
            totais.getCell(1).setCellStyle(moeda);
            totais.createCell(2).setCellValue("Saídas");
            totais.createCell(3).setCellValue(resumo.totalSaidas().doubleValue());
            totais.getCell(3).setCellStyle(moeda);
            totais.createCell(4).setCellValue("Saldo");
            totais.createCell(5).setCellValue(resumo.saldo().doubleValue());
            totais.getCell(5).setCellStyle(moeda);

            Row titulos = planilha.createRow(7);
            String[] colunas = {"Data", "Contexto", "Descrição", "Categoria", "Tipo", "Valor"};
            for (int indice = 0; indice < colunas.length; indice++) {
                titulos.createCell(indice).setCellValue(colunas[indice]);
                titulos.getCell(indice).setCellStyle(cabecalho);
            }
            int numeroLinha = 8;
            for (RelatorioTransacaoResponse transacao : resumo.transacoes()) {
                Row item = planilha.createRow(numeroLinha++);
                item.createCell(0).setCellValue(java.sql.Date.valueOf(transacao.data()));
                item.getCell(0).setCellStyle(data);
                item.createCell(1).setCellValue(transacao.contexto());
                item.createCell(2).setCellValue(transacao.descricao());
                item.createCell(3).setCellValue(transacao.categoria());
                item.createCell(4).setCellValue(transacao.tipo().name());
                item.createCell(5).setCellValue(transacao.valor().doubleValue());
                item.getCell(5).setCellStyle(moeda);
            }
            planilha.createFreezePane(0, 8);
            planilha.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                    7, Math.max(7, numeroLinha - 1), 0, 5));
            for (int indice = 0; indice < colunas.length; indice++) {
                planilha.autoSizeColumn(indice);
                planilha.setColumnWidth(indice,
                        Math.min(planilha.getColumnWidth(indice) + 512, 15000));
            }
            workbook.write(saida);
            return saida.toByteArray();
        }
    }

    private CellStyle estiloTitulo(Workbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        Font fonte = workbook.createFont();
        fonte.setBold(true);
        fonte.setFontHeightInPoints((short) 16);
        fonte.setColor(IndexedColors.DARK_RED.getIndex());
        estilo.setFont(fonte);
        return estilo;
    }

    private CellStyle estiloCabecalho(Workbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        Font fonte = workbook.createFont();
        fonte.setBold(true);
        fonte.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(fonte);
        estilo.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilo.setAlignment(HorizontalAlignment.CENTER);
        return estilo;
    }

    private CellStyle estiloMoeda(Workbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        estilo.setDataFormat(workbook.createDataFormat().getFormat("R$ #,##0.00"));
        return estilo;
    }

    private CellStyle estiloData(Workbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        estilo.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy"));
        return estilo;
    }

    private void validarPeriodo(LocalDate dataInicial, LocalDate dataFinal) {
        if (dataInicial == null || dataFinal == null) {
            throw new RegraNegocioException(
                    HttpStatus.BAD_REQUEST, "periodo-obrigatorio",
                    "Informe as datas inicial e final do relatório."
            );
        }
        if (dataInicial.isAfter(dataFinal)) {
            throw new RegraNegocioException(
                    HttpStatus.BAD_REQUEST, "periodo-invalido",
                    "A data inicial não pode ser posterior à data final."
            );
        }
    }

    private String moeda(BigDecimal valor) {
        return "R$ " + valor.setScale(2, RoundingMode.HALF_UP)
                .toPlainString().replace('.', ',');
    }

    private String limitar(String texto, int limite) {
        return texto.length() <= limite ? texto : texto.substring(0, limite - 1) + ".";
    }

    public record ArquivoRelatorio(
            byte[] conteudo,
            MediaType mediaType,
            String nomeArquivo
    ) {
    }

    private record DadosRelatorio(
            RelatorioResumoResponse resumo,
            List<Transacao> transacoes
    ) {
    }

    private static class TotaisPeriodo {
        private BigDecimal entradas = ZERO;
        private BigDecimal saidas = ZERO;
    }

    private static class TotaisCategoria {
        private long quantidade;
        private BigDecimal total = ZERO;
    }

    private static class EscritorPdf implements AutoCloseable {
        private static final PDType1Font NORMAL = new PDType1Font(
                Standard14Fonts.FontName.HELVETICA);
        private static final PDType1Font NEGRITO = new PDType1Font(
                Standard14Fonts.FontName.HELVETICA_BOLD);

        private final PDDocument documento;
        private PDPageContentStream conteudo;
        private float y;

        private EscritorPdf(PDDocument documento) throws IOException {
            this.documento = documento;
            novaPagina();
        }

        private void novaPagina() throws IOException {
            fecharConteudo();
            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            conteudo = new PDPageContentStream(documento, pagina);
            y = 800;
        }

        private void titulo(String texto) throws IOException {
            escrever(texto, 17, NEGRITO);
            espaco(8);
        }

        private void texto(String texto, int tamanho) throws IOException {
            escrever(texto, tamanho, NORMAL);
        }

        private void escrever(String texto, int tamanho, PDType1Font fonte) throws IOException {
            if (y < 55) {
                novaPagina();
            }
            conteudo.beginText();
            conteudo.setFont(fonte, tamanho);
            conteudo.newLineAtOffset(42, y);
            conteudo.showText(textoPdf(texto));
            conteudo.endText();
            y -= tamanho + 5;
        }

        private void linha() throws IOException {
            conteudo.moveTo(42, y + 6);
            conteudo.lineTo(553, y + 6);
            conteudo.stroke();
            y -= 5;
        }

        private void espaco(int espaco) {
            y -= espaco;
        }

        private String textoPdf(String texto) {
            return new String(texto.getBytes(StandardCharsets.ISO_8859_1),
                    StandardCharsets.ISO_8859_1)
                    .replaceAll("[^\\x20-\\xFF]", "?");
        }

        private void fecharConteudo() throws IOException {
            if (conteudo != null) {
                conteudo.close();
                conteudo = null;
            }
        }

        @Override
        public void close() throws IOException {
            fecharConteudo();
        }
    }
}
