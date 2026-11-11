package br.edu.ifro.ji.service;

import br.edu.ifro.ji.exception.RegraNegocioException;
import br.edu.ifro.ji.model.FormatoImportacao;
import br.edu.ifro.ji.model.TipoTransacao;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LeitorImportacaoService {

    private static final long TAMANHO_MAXIMO = 10L * 1024 * 1024;
    private static final int LIMITE_REGISTROS = 1000;
    private static final Pattern LINHA_PDF = Pattern.compile(
            "^(\\d{2}/\\d{2}/\\d{4}|\\d{4}-\\d{2}-\\d{2})\\s+(.+?)\\s+" +
                    "(-?\\s*(?:R\\$\\s*)?[\\d.]+(?:,\\d{2}|\\.\\d{2}))" +
                    "(?:\\s+(ENTRADA|SA[IÍ]DA))?\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final List<DateTimeFormatter> FORMATOS_DATA = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/uuuu"),
            DateTimeFormatter.ofPattern("dd-MM-uuuu")
    );

    public ArquivoLido ler(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw erro("arquivo-vazio", "Selecione um arquivo PDF ou XLSX para continuar.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw erro("arquivo-muito-grande", "O arquivo deve ter no máximo 10 MB.");
        }

        String nome = nomeSeguro(arquivo.getOriginalFilename());
        FormatoImportacao formato = formato(nome);
        try {
            List<RegistroLido> registros = formato == FormatoImportacao.XLSX
                    ? lerXlsx(arquivo.getBytes())
                    : lerPdf(arquivo.getBytes());
            if (registros.isEmpty()) {
                throw erro("arquivo-sem-registros",
                        "Nenhum registro financeiro válido foi encontrado no arquivo.");
            }
            if (registros.size() > LIMITE_REGISTROS) {
                throw erro("arquivo-com-muitos-registros",
                        "Cada importação aceita no máximo 1.000 registros.");
            }
            return new ArquivoLido(nome, formato, registros);
        } catch (RegraNegocioException exception) {
            throw exception;
        } catch (Exception exception) {
            throw erro("arquivo-invalido",
                    "Não foi possível ler o arquivo. Verifique se ele não está corrompido.");
        }
    }

    private List<RegistroLido> lerXlsx(byte[] conteudo) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(conteudo))) {
            if (workbook.getNumberOfSheets() == 0) {
                return List.of();
            }
            Sheet planilha = workbook.getSheetAt(0);
            Row cabecalho = planilha.getRow(planilha.getFirstRowNum());
            if (cabecalho == null) {
                return List.of();
            }
            Map<String, Integer> colunas = colunas(cabecalho);
            validarColunas(colunas);
            List<RegistroLido> registros = new ArrayList<>();
            for (int indice = cabecalho.getRowNum() + 1;
                 indice <= planilha.getLastRowNum(); indice++) {
                Row linha = planilha.getRow(indice);
                if (linha == null || linhaVazia(linha)) {
                    continue;
                }
                int numero = indice + 1;
                LocalDate data = data(linha.getCell(colunas.get("data")), numero);
                String descricao = texto(linha.getCell(colunas.get("descricao"))).trim();
                if (descricao.isBlank() || descricao.length() > 255) {
                    throw conteudoInvalido(numero,
                            "a descrição é obrigatória e deve ter no máximo 255 caracteres");
                }
                BigDecimal valorOriginal = valor(linha.getCell(colunas.get("valor")), numero);
                TipoTransacao tipo = tipo(texto(linha.getCell(colunas.get("tipo"))),
                        valorOriginal, numero, true);
                registros.add(new RegistroLido(
                        numero, data, normalizarDescricao(descricao),
                        valorOriginal.abs().setScale(2, RoundingMode.HALF_UP), tipo
                ));
            }
            return registros;
        }
    }

    private List<RegistroLido> lerPdf(byte[] conteudo) throws IOException {
        try (PDDocument documento = Loader.loadPDF(conteudo)) {
            String texto = new PDFTextStripper().getText(documento);
            if (texto == null || texto.isBlank()) {
                throw erro("pdf-sem-texto",
                        "O PDF não possui texto selecionável. Use um extrato digital, não uma imagem digitalizada.");
            }
            List<RegistroLido> registros = new ArrayList<>();
            String[] linhas = texto.split("\\R");
            for (int indice = 0; indice < linhas.length; indice++) {
                String linha = linhas[indice].trim();
                if (linha.isBlank() || ehCabecalho(linha)) {
                    continue;
                }
                RegistroLido registro = registroPdf(linha, indice + 1);
                if (registro != null) {
                    registros.add(registro);
                } else if (linha.matches("^(\\d{2}/\\d{2}/\\d{4}|\\d{4}-\\d{2}-\\d{2}).*")) {
                    throw conteudoInvalido(indice + 1,
                            "use data, descrição, valor e tipo, separados por espaço, ponto e vírgula ou barra vertical");
                }
            }
            return registros;
        }
    }

    private RegistroLido registroPdf(String linha, int numero) {
        String[] campos = linha.split("\\s*[;|\\t]\\s*");
        if (campos.length >= 4) {
            LocalDate data = data(campos[0], numero);
            String descricao = normalizarDescricao(campos[1]);
            BigDecimal valorOriginal = valor(campos[2], numero);
            TipoTransacao tipo = tipo(campos[3], valorOriginal, numero, false);
            validarDescricaoPdf(descricao, numero);
            return new RegistroLido(numero, data, descricao,
                    valorOriginal.abs().setScale(2, RoundingMode.HALF_UP), tipo);
        }

        Matcher correspondencia = LINHA_PDF.matcher(linha);
        if (!correspondencia.matches()) {
            return null;
        }
        LocalDate data = data(correspondencia.group(1), numero);
        String descricao = normalizarDescricao(correspondencia.group(2));
        BigDecimal valorOriginal = valor(correspondencia.group(3), numero);
        TipoTransacao tipo = tipo(correspondencia.group(4), valorOriginal, numero, false);
        validarDescricaoPdf(descricao, numero);
        return new RegistroLido(numero, data, descricao,
                valorOriginal.abs().setScale(2, RoundingMode.HALF_UP), tipo);
    }

    private Map<String, Integer> colunas(Row cabecalho) {
        Map<String, Integer> colunas = new HashMap<>();
        for (Cell celula : cabecalho) {
            colunas.put(normalizar(texto(celula)), celula.getColumnIndex());
        }
        return colunas;
    }

    private void validarColunas(Map<String, Integer> colunas) {
        List<String> obrigatorias = List.of("data", "descricao", "valor", "tipo");
        List<String> ausentes = obrigatorias.stream()
                .filter(coluna -> !colunas.containsKey(coluna))
                .toList();
        if (!ausentes.isEmpty()) {
            throw erro("colunas-obrigatorias-ausentes",
                    "A planilha deve conter as colunas data, descrição, valor e tipo.");
        }
    }

    private boolean linhaVazia(Row linha) {
        DataFormatter formatador = new DataFormatter(Locale.forLanguageTag("pt-BR"));
        for (Cell celula : linha) {
            if (!formatador.formatCellValue(celula).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private LocalDate data(Cell celula, int linha) {
        if (celula != null && celula.getCellType() == CellType.NUMERIC
                && DateUtil.isCellDateFormatted(celula)) {
            return celula.getLocalDateTimeCellValue().toLocalDate();
        }
        return data(texto(celula), linha);
    }

    private LocalDate data(String valor, int linha) {
        String texto = valor == null ? "" : valor.trim();
        for (DateTimeFormatter formato : FORMATOS_DATA) {
            try {
                LocalDate data = LocalDate.parse(texto, formato);
                if (data.isAfter(LocalDate.now())) {
                    throw conteudoInvalido(linha, "a data não pode estar no futuro");
                }
                return data;
            } catch (DateTimeParseException ignored) {
                // Tenta o próximo formato permitido.
            }
        }
        throw conteudoInvalido(linha, "a data deve usar AAAA-MM-DD ou DD/MM/AAAA");
    }

    private BigDecimal valor(Cell celula, int linha) {
        if (celula != null && celula.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(celula.getNumericCellValue());
        }
        return valor(texto(celula), linha);
    }

    private BigDecimal valor(String informado, int linha) {
        if (informado == null || informado.isBlank()) {
            throw conteudoInvalido(linha, "o valor é obrigatório");
        }
        String normalizado = informado.trim()
                .replace("R$", "")
                .replace(" ", "");
        boolean parenteses = normalizado.startsWith("(") && normalizado.endsWith(")");
        normalizado = normalizado.replace("(", "").replace(")", "");
        if (normalizado.contains(",")) {
            normalizado = normalizado.replace(".", "").replace(',', '.');
        }
        try {
            BigDecimal valor = new BigDecimal(normalizado);
            if (parenteses) {
                valor = valor.negate();
            }
            if (valor.signum() == 0) {
                throw conteudoInvalido(linha, "o valor deve ser diferente de zero");
            }
            return valor;
        } catch (NumberFormatException exception) {
            throw conteudoInvalido(linha, "o valor possui formato inválido");
        }
    }

    private TipoTransacao tipo(String informado, BigDecimal valor, int linha,
                               boolean obrigatorio) {
        String normalizado = normalizar(informado == null ? "" : informado);
        if (normalizado.equals("entrada") || normalizado.equals("credito")) {
            return TipoTransacao.ENTRADA;
        }
        if (normalizado.equals("saida") || normalizado.equals("debito")) {
            return TipoTransacao.SAIDA;
        }
        if (!obrigatorio && normalizado.isBlank()) {
            return valor.signum() < 0 ? TipoTransacao.SAIDA : TipoTransacao.ENTRADA;
        }
        throw conteudoInvalido(linha, "o tipo deve ser ENTRADA ou SAÍDA");
    }

    private String texto(Cell celula) {
        if (celula == null) {
            return "";
        }
        return new DataFormatter(Locale.forLanguageTag("pt-BR")).formatCellValue(celula);
    }

    private boolean ehCabecalho(String linha) {
        String normalizada = normalizar(linha);
        return normalizada.contains("data") && normalizada.contains("descricao")
                && normalizada.contains("valor");
    }

    private void validarDescricaoPdf(String descricao, int linha) {
        if (descricao.isBlank() || descricao.length() > 255) {
            throw conteudoInvalido(linha,
                    "a descrição é obrigatória e deve ter no máximo 255 caracteres");
        }
    }

    private String nomeSeguro(String original) {
        String nome = original == null ? "" : original.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1).trim();
        if (nome.isBlank() || nome.length() > 255) {
            throw erro("nome-arquivo-invalido", "O nome do arquivo é inválido.");
        }
        return nome;
    }

    private FormatoImportacao formato(String nome) {
        String minusculo = nome.toLowerCase(Locale.ROOT);
        if (minusculo.endsWith(".xlsx")) {
            return FormatoImportacao.XLSX;
        }
        if (minusculo.endsWith(".pdf")) {
            return FormatoImportacao.PDF;
        }
        throw erro("formato-arquivo-invalido", "Envie um arquivo no formato PDF ou XLSX.");
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String normalizarDescricao(String descricao) {
        return descricao.trim().replaceAll("\\s+", " ");
    }

    private RegraNegocioException conteudoInvalido(int linha, String detalhe) {
        return erro("arquivo-conteudo-invalido", "Linha " + linha + ": " + detalhe + ".");
    }

    private RegraNegocioException erro(String codigo, String mensagem) {
        return new RegraNegocioException(HttpStatus.BAD_REQUEST, codigo, mensagem);
    }

    public record ArquivoLido(
            String nome,
            FormatoImportacao formato,
            List<RegistroLido> registros
    ) {
    }

    public record RegistroLido(
            int linha,
            LocalDate data,
            String descricao,
            BigDecimal valor,
            TipoTransacao tipo
    ) {
    }
}
