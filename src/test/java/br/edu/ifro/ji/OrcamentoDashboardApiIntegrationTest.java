package br.edu.ifro.ji;

import br.edu.ifro.ji.model.TipoCategoria;
import br.edu.ifro.ji.repository.CategoriaRepository;
import br.edu.ifro.ji.repository.ConfiguracaoUsuarioRepository;
import br.edu.ifro.ji.repository.ContextoParticipanteRepository;
import br.edu.ifro.ji.repository.ContextoRepository;
import br.edu.ifro.ji.repository.LogAuditoriaRepository;
import br.edu.ifro.ji.repository.OrcamentoCategoriaRepository;
import br.edu.ifro.ji.repository.SessaoRepository;
import br.edu.ifro.ji.repository.TokenRedefinicaoSenhaRepository;
import br.edu.ifro.ji.repository.TransacaoRepository;
import br.edu.ifro.ji.repository.UsuarioRepository;
import br.edu.ifro.ji.service.PasswordResetMailSender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(OrcamentoDashboardApiIntegrationTest.MailTestConfig.class)
class OrcamentoDashboardApiIntegrationTest {

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
    private OrcamentoCategoriaRepository orcamentoRepository;
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
        orcamentoRepository.deleteAllInBatch();
        transacaoRepository.deleteAllInBatch();
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
    void criaListaEditaEExcluiOrcamentoComAlertasDeConsumo() throws Exception {
        cadastrar("filipe@example.com");
        String token = login("filipe@example.com");
        long alimentacaoId = idCategoria(token, "Alimentação");
        YearMonth mes = YearMonth.now(ZoneOffset.UTC);

        criarTransacao(token, "SAIDA", "80.00", dataValidaNoMes(mes, 1),
                alimentacaoId, "Mercado");
        criarTransacao(token, "ENTRADA", "500.00", dataValidaNoMes(mes, 2),
                alimentacaoId, "Reembolso");

        String criado = mvc.perform(post("/api/v1/orcamentos")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orcamentoJson(alimentacaoId, "100.00", mes)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("/api/v1/orcamentos/")))
                .andExpect(jsonPath("$.valorConsumido").value(80.00))
                .andExpect(jsonPath("$.percentualConsumido").value(80.00))
                .andExpect(jsonPath("$.situacao").value("ATENCAO"))
                .andReturn().getResponse().getContentAsString();
        long orcamentoId = objectMapper.readTree(criado).path("id").asLong();

        mvc.perform(post("/api/v1/orcamentos")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orcamentoJson(alimentacaoId, "200.00", mes)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("orcamento-duplicado"));

        mvc.perform(get("/api/v1/orcamentos")
                        .header("Authorization", bearer(token))
                        .queryParam("mes", mes.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLimite").value(100.00))
                .andExpect(jsonPath("$.totalConsumido").value(80.00))
                .andExpect(jsonPath("$.itens.length()").value(1));

        mvc.perform(put("/api/v1/orcamentos/{id}", orcamentoId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orcamentoJson(alimentacaoId, "75.00", mes)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentualConsumido").value(106.67))
                .andExpect(jsonPath("$.situacao").value("EXCEDIDO"));

        mvc.perform(delete("/api/v1/orcamentos/{id}", orcamentoId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        assertThat(orcamentoRepository.findById(orcamentoId)).isEmpty();
        assertThat(auditoriaRepository.findAll()).extracting("acao")
                .contains("CRIACAO_ORCAMENTO", "ATUALIZACAO_ORCAMENTO", "EXCLUSAO_ORCAMENTO");
    }

    @Test
    void dashboardResumeMesComFluxoCategoriasRecentesEEstadoVazio() throws Exception {
        cadastrar("filipe@example.com");
        String token = login("filipe@example.com");
        long salarioId = idCategoria(token, "Salário");
        long alimentacaoId = idCategoria(token, "Alimentação");
        long lazerId = idCategoria(token, "Lazer");
        YearMonth mes = YearMonth.now(ZoneOffset.UTC);

        criarTransacao(token, "ENTRADA", "1000.00", dataValidaNoMes(mes, 1),
                salarioId, "Salário");
        criarTransacao(token, "SAIDA", "100.00", dataValidaNoMes(mes, 2),
                alimentacaoId, "Mercado");
        criarTransacao(token, "SAIDA", "50.00", dataValidaNoMes(mes, 3),
                lazerId, "Cinema");

        mvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", bearer(token))
                        .queryParam("mes", mes.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntradas").value(1000.00))
                .andExpect(jsonPath("$.totalSaidas").value(150.00))
                .andExpect(jsonPath("$.saldo").value(850.00))
                .andExpect(jsonPath("$.fluxoDiario.length()").value(3))
                .andExpect(jsonPath("$.fluxoDiario[0].saldoAcumulado").value(1000.00))
                .andExpect(jsonPath("$.fluxoDiario[2].saldoAcumulado").value(850.00))
                .andExpect(jsonPath("$.principaisCategorias[0].categoriaNome")
                        .value("Alimentação"))
                .andExpect(jsonPath("$.principaisCategorias[0].percentual").value(66.67))
                .andExpect(jsonPath("$.transacoesRecentes[0].descricao").value("Cinema"))
                .andExpect(jsonPath("$.geradoEm").exists());

        mvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", bearer(token))
                        .queryParam("mes", mes.minusMonths(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntradas").value(0))
                .andExpect(jsonPath("$.totalSaidas").value(0))
                .andExpect(jsonPath("$.fluxoDiario").isEmpty())
                .andExpect(jsonPath("$.principaisCategorias").isEmpty())
                .andExpect(jsonPath("$.transacoesRecentes").isEmpty());
    }

    @Test
    void validaMesCategoriaEIsolaOrcamentosPorContexto() throws Exception {
        cadastrar("filipe@example.com");
        cadastrar("ana@example.com");
        String tokenFilipe = login("filipe@example.com");
        String tokenAna = login("ana@example.com");
        long personalizadaFilipeId = criarCategoria(tokenFilipe, "Casa");
        YearMonth mes = YearMonth.now(ZoneOffset.UTC);

        mvc.perform(post("/api/v1/orcamentos")
                        .header("Authorization", bearer(tokenAna))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orcamentoJson(personalizadaFilipeId, "100.00", mes)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("categoria-indisponivel"));

        String criado = mvc.perform(post("/api/v1/orcamentos")
                        .header("Authorization", bearer(tokenFilipe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orcamentoJson(personalizadaFilipeId, "100.00", mes)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long orcamentoId = objectMapper.readTree(criado).path("id").asLong();

        mvc.perform(delete("/api/v1/orcamentos/{id}", orcamentoId)
                        .header("Authorization", bearer(tokenAna)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("orcamento-nao-encontrado"));

        mvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", bearer(tokenFilipe))
                        .queryParam("mes", "2026-13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("mes-referencia-invalido"));

        mvc.perform(post("/api/v1/orcamentos")
                        .header("Authorization", bearer(tokenFilipe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orcamentoJson(personalizadaFilipeId, "0", mes)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.valorLimite").exists());

        mvc.perform(delete("/api/v1/categorias/{id}", personalizadaFilipeId)
                        .header("Authorization", bearer(tokenFilipe)))
                .andExpect(status().isNoContent());
        assertThat(categoriaRepository.findById(personalizadaFilipeId)
                .orElseThrow().getDataExclusao()).isNotNull();
    }

    private LocalDate dataValidaNoMes(YearMonth mes, int diaPreferido) {
        LocalDate hoje = LocalDate.now(ZoneOffset.UTC);
        int ultimoDiaPermitido = mes.equals(YearMonth.from(hoje))
                ? hoje.getDayOfMonth()
                : mes.lengthOfMonth();
        return mes.atDay(Math.min(diaPreferido, ultimoDiaPermitido));
    }

    private void cadastrar(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Pessoa",
                                "sobrenome", "Teste",
                                "email", email,
                                "senha", "senha-segura"
                        ))))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        String resposta = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "senha", "senha-segura"
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

    private long criarCategoria(String token, String nome) throws Exception {
        String resposta = mvc.perform(post("/api/v1/categorias")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", nome))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resposta).path("id").asLong();
    }

    private void criarTransacao(String token, String tipo, String valor, LocalDate data,
                                long categoriaId, String descricao) throws Exception {
        mvc.perform(post("/api/v1/transacoes")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transacaoJson(tipo, valor, data, categoriaId, descricao)))
                .andExpect(status().isCreated());
    }

    private String transacaoJson(String tipo, String valor, LocalDate data,
                                 long categoriaId, String descricao) throws Exception {
        LinkedHashMap<String, Object> dados = new LinkedHashMap<>();
        dados.put("tipo", tipo);
        dados.put("valor", new BigDecimal(valor));
        dados.put("data", data.toString());
        dados.put("categoriaId", categoriaId);
        dados.put("descricao", descricao);
        return objectMapper.writeValueAsString(dados);
    }

    private String orcamentoJson(long categoriaId, String valor,
                                 YearMonth mes) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "categoriaId", categoriaId,
                "valorLimite", new BigDecimal(valor),
                "mesReferencia", mes.toString()
        ));
    }

    private String bearer(String token) {
        return "Bearer " + token;
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
