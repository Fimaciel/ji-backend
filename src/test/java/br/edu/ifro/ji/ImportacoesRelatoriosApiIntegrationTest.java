package br.edu.ifro.ji;

import br.edu.ifro.ji.model.TipoCategoria;
import br.edu.ifro.ji.repository.CategoriaRepository;
import br.edu.ifro.ji.repository.ConfiguracaoUsuarioRepository;
import br.edu.ifro.ji.repository.ContextoParticipanteRepository;
import br.edu.ifro.ji.repository.ContextoRepository;
import br.edu.ifro.ji.repository.ImportacaoArquivoRepository;
import br.edu.ifro.ji.repository.LogAuditoriaRepository;
import br.edu.ifro.ji.repository.SessaoRepository;
import br.edu.ifro.ji.repository.TokenRedefinicaoSenhaRepository;
import br.edu.ifro.ji.repository.TransacaoRepository;
import br.edu.ifro.ji.repository.UsuarioRepository;
import br.edu.ifro.ji.service.PasswordResetMailSender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(ImportacoesRelatoriosApiIntegrationTest.MailTestConfig.class)
class ImportacoesRelatoriosApiIntegrationTest {

    private static final String SEGREDO_JWT = Base64.getEncoder().encodeToString(
            "segredo-jwt-de-testes-com-32-bytes".getBytes(StandardCharsets.UTF_8));

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registry) {
        registry.add("ji.security.jwt.secret-base64", () -> SEGREDO_JWT);
        registry.add("ji.mail.mode", () -> "test");
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ImportacaoArquivoRepository importacaoRepository;
    @Autowired
    private TransacaoRepository transacaoRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private LogAuditoriaRepository auditoriaRepository;
    @Autowired
    private TokenRedefinicaoSenhaRepository tokenRepository;
    @Autowired
    private SessaoRepository sessaoRepository;
    @Autowired
    private ContextoParticipanteRepository participanteRepository;
    @Autowired
    private ContextoRepository contextoRepository;
    @Autowired
    private ConfiguracaoUsuarioRepository configuracaoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void limparBanco() {
        auditoriaRepository.deleteAllInBatch();
        transacaoRepository.deleteAllInBatch();
        importacaoRepository.deleteAllInBatch();
        List<br.edu.ifro.ji.model.Categoria> personalizadas = categoriaRepository.findAll()
                .stream()
                .filter(categoria -> categoria.getTipo() == TipoCategoria.PERSONALIZADA)
                .toList();
        categoriaRepository.deleteAllInBatch(personalizadas);
        tokenRepository.deleteAllInBatch();
        sessaoRepository.deleteAllInBatch();
        participanteRepository.deleteAllInBatch();
        contextoRepository.deleteAllInBatch();
        configuracaoRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
    }

    @Test
    void analisaConfirmaIgnoraDuplicadosListaEDesfazImportacaoXlsx() throws Exception {
        cadastrar("filipe@example.com");
        String token = login("filipe@example.com");
        LocalDate data = LocalDate.now().minusDays(1);
        long alimentacaoId = idCategoria(token, "Alimentação");
        criarTransacao(token, "SAIDA", "50.00", data, alimentacaoId, "Mercado do bairro");

        byte[] planilha = planilha(List.of(
                new LinhaArquivo(data, "Mercado do bairro", "50.00", "SAIDA"),
                new LinhaArquivo(data, "Salário mensal", "3500.00", "ENTRADA"),
                new LinhaArquivo(data, "Farmácia Central", "85.40", "SAIDA")
        ));
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "extrato-novembro.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", planilha);
        String analiseJson = mvc.perform(multipart("/api/v1/importacoes/analise")
                        .file(arquivo)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLidos").value(3))
                .andExpect(jsonPath("$.totalNovos").value(2))
                .andExpect(jsonPath("$.totalDuplicados").value(1))
                .andExpect(jsonPath("$.registros[0].duplicado").value(true))
                .andExpect(jsonPath("$.registros[1].categoriaSugerida.nome").value("Salário"))
                .andExpect(jsonPath("$.registros[2].categoriaSugerida.nome").value("Saúde"))
                .andReturn().getResponse().getContentAsString();
        JsonNode analise = objectMapper.readTree(analiseJson);
        List<Map<String, Object>> registros = new ArrayList<>();
        analise.path("registros").forEach(registro -> {
            if (!registro.path("duplicado").asBoolean()) {
                registros.add(Map.of(
                        "data", registro.path("data").asText(),
                        "descricao", registro.path("descricao").asText(),
                        "valor", registro.path("valor").decimalValue(),
                        "tipo", registro.path("tipo").asText(),
                        "categoriaId", registro.path("categoriaSugerida").path("id").asLong()
                ));
            }
        });
        String confirmacao = objectMapper.writeValueAsString(Map.of(
                "nomeArquivo", analise.path("nomeArquivo").asText(),
                "formato", analise.path("formato").asText(),
                "totalLidos", 3,
                "totalDuplicados", 1,
                "registros", registros
        ));

        mvc.perform(post("/api/v1/importacoes")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmacao))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("/api/v1/importacoes/")))
                .andExpect(jsonPath("$.totalImportados").value(2))
                .andExpect(jsonPath("$.totalDuplicados").value(1));

        assertThat(transacaoRepository.count()).isEqualTo(3);
        mvc.perform(get("/api/v1/importacoes")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CONCLUIDA"));

        mvc.perform(delete("/api/v1/importacoes/ultima")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DESFEITA"));
        assertThat(transacaoRepository.count()).isEqualTo(1);
        assertThat(auditoriaRepository.findAll()).extracting("acao")
                .contains("IMPORTACAO_TRANSACOES", "DESFAZIMENTO_IMPORTACAO");
    }

    @Test
    void aceitaPdfComTextoERejeitaPdfDigitalizado() throws Exception {
        cadastrar("filipe@example.com");
        String token = login("filipe@example.com");
        LocalDate data = LocalDate.now().minusDays(2);

        MockMultipartFile textual = new MockMultipartFile(
                "arquivo", "extrato.pdf", MediaType.APPLICATION_PDF_VALUE,
                pdfComTexto(data + "; Cinema; -35,90; SAIDA"));
        mvc.perform(multipart("/api/v1/importacoes/analise")
                        .file(textual)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLidos").value(1))
                .andExpect(jsonPath("$.registros[0].descricao").value("Cinema"))
                .andExpect(jsonPath("$.registros[0].categoriaSugerida.nome").value("Lazer"));

        MockMultipartFile digitalizado = new MockMultipartFile(
                "arquivo", "digitalizado.pdf", MediaType.APPLICATION_PDF_VALUE, pdfVazio());
        mvc.perform(multipart("/api/v1/importacoes/analise")
                        .file(digitalizado)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("pdf-sem-texto"));
    }

    @Test
    void resumeEExportaRelatorioEmPdfEXlsx() throws Exception {
        cadastrar("filipe@example.com");
        String token = login("filipe@example.com");
        long salarioId = idCategoria(token, "Salário");
        long alimentacaoId = idCategoria(token, "Alimentação");
        LocalDate data = LocalDate.now().minusDays(1);
        criarTransacao(token, "ENTRADA", "1000.00", data, salarioId, "Salário");
        criarTransacao(token, "SAIDA", "150.00", data, alimentacaoId, "Mercado");

        mvc.perform(get("/api/v1/relatorios/resumo")
                        .header("Authorization", bearer(token))
                        .queryParam("dataInicial", data.minusDays(1).toString())
                        .queryParam("dataFinal", data.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntradas").value(1000.00))
                .andExpect(jsonPath("$.totalSaidas").value(150.00))
                .andExpect(jsonPath("$.saldo").value(850.00))
                .andExpect(jsonPath("$.quantidadeTransacoes").value(2))
                .andExpect(jsonPath("$.fluxoMensal.length()").value(1))
                .andExpect(jsonPath("$.categorias.length()").value(2));

        byte[] pdf = mvc.perform(get("/api/v1/relatorios/exportacao")
                        .header("Authorization", bearer(token))
                        .queryParam("dataInicial", data.minusDays(1).toString())
                        .queryParam("dataFinal", data.plusDays(1).toString())
                        .queryParam("formato", "PDF"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");

        byte[] xlsx = mvc.perform(get("/api/v1/relatorios/exportacao")
                        .header("Authorization", bearer(token))
                        .queryParam("dataInicial", data.minusDays(1).toString())
                        .queryParam("dataFinal", data.plusDays(1).toString())
                        .queryParam("tipo", "SAIDA")
                        .queryParam("formato", "XLSX"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(xlsx))) {
            assertThat(workbook.getSheet("Relatório").getRow(8).getCell(2).getStringCellValue())
                    .isEqualTo("Mercado");
        }
    }

    @Test
    void naoExportaRelatorioSemDados() throws Exception {
        cadastrar("filipe@example.com");
        String token = login("filipe@example.com");
        LocalDate data = LocalDate.now().minusYears(2);
        mvc.perform(get("/api/v1/relatorios/exportacao")
                        .header("Authorization", bearer(token))
                        .queryParam("dataInicial", data.toString())
                        .queryParam("dataFinal", data.plusDays(1).toString())
                        .queryParam("formato", "PDF"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("relatorio-sem-dados"));
    }

    private byte[] planilha(List<LinhaArquivo> linhas) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Extrato");
            Row cabecalho = sheet.createRow(0);
            cabecalho.createCell(0).setCellValue("data");
            cabecalho.createCell(1).setCellValue("descrição");
            cabecalho.createCell(2).setCellValue("valor");
            cabecalho.createCell(3).setCellValue("tipo");
            int indice = 1;
            for (LinhaArquivo dados : linhas) {
                Row linha = sheet.createRow(indice++);
                linha.createCell(0).setCellValue(dados.data().toString());
                linha.createCell(1).setCellValue(dados.descricao());
                linha.createCell(2).setCellValue(new BigDecimal(dados.valor()).doubleValue());
                linha.createCell(3).setCellValue(dados.tipo());
            }
            workbook.write(saida);
            return saida.toByteArray();
        }
    }

    private byte[] pdfComTexto(String texto) throws Exception {
        try (PDDocument documento = new PDDocument();
             ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage();
            documento.addPage(pagina);
            try (PDPageContentStream conteudo = new PDPageContentStream(documento, pagina)) {
                conteudo.beginText();
                conteudo.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                conteudo.newLineAtOffset(40, 700);
                conteudo.showText(texto);
                conteudo.endText();
            }
            documento.save(saida);
            return saida.toByteArray();
        }
    }

    private byte[] pdfVazio() throws Exception {
        try (PDDocument documento = new PDDocument();
             ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            documento.addPage(new PDPage());
            documento.save(saida);
            return saida.toByteArray();
        }
    }

    private void cadastrar(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Pessoa", "sobrenome", "Teste",
                                "email", email, "senha", "senha-segura"
                        ))))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        String resposta = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "senha", "senha-segura"
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resposta).path("tokenAcesso").asText();
    }

    private long idCategoria(String token, String nome) throws Exception {
        JsonNode categorias = objectMapper.readTree(mvc.perform(get("/api/v1/categorias")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        for (JsonNode categoria : categorias) {
            if (nome.equals(categoria.path("nome").asText())) {
                return categoria.path("id").asLong();
            }
        }
        throw new AssertionError("Categoria não encontrada: " + nome);
    }

    private void criarTransacao(String token, String tipo, String valor, LocalDate data,
                                long categoriaId, String descricao) throws Exception {
        LinkedHashMap<String, Object> dados = new LinkedHashMap<>();
        dados.put("tipo", tipo);
        dados.put("valor", new BigDecimal(valor));
        dados.put("data", data.toString());
        dados.put("categoriaId", categoriaId);
        dados.put("descricao", descricao);
        mvc.perform(post("/api/v1/transacoes")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dados)))
                .andExpect(status().isCreated());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record LinhaArquivo(
            LocalDate data,
            String descricao,
            String valor,
            String tipo
    ) {
    }

    @TestConfiguration
    static class MailTestConfig {
        @Bean
        @Primary
        PasswordResetMailSender passwordResetMailSender() {
            return (destinatario, nome, linkRedefinicao) -> { };
        }
    }
}
