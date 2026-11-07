package br.edu.ifro.ji;

import br.edu.ifro.ji.model.TipoCategoria;
import br.edu.ifro.ji.repository.CategoriaRepository;
import br.edu.ifro.ji.repository.ConfiguracaoUsuarioRepository;
import br.edu.ifro.ji.repository.ContextoParticipanteRepository;
import br.edu.ifro.ji.repository.ContextoRepository;
import br.edu.ifro.ji.repository.LogAuditoriaRepository;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
@Import(TransacoesCategoriasApiIntegrationTest.MailTestConfig.class)
class TransacoesCategoriasApiIntegrationTest {

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
    void gerenciaCategoriasPersonalizadasSemAlterarAsPadrao() throws Exception {
        cadastrar("filipe@example.com");
        String token = login("filipe@example.com");

        String categorias = mvc.perform(get("/api/v1/categorias")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[?(@.nome == 'Alimentação')]").exists())
                .andExpect(jsonPath("$[*].tipo").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is("PADRAO"))))
                .andReturn().getResponse().getContentAsString();
        long categoriaPadraoId = objectMapper.readTree(categorias).get(0).path("id").asLong();

        String criada = mvc.perform(post("/api/v1/categorias")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"  Assinaturas digitais  \"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("/api/v1/categorias/")))
                .andExpect(jsonPath("$.nome").value("Assinaturas digitais"))
                .andExpect(jsonPath("$.tipo").value("PERSONALIZADA"))
                .andReturn().getResponse().getContentAsString();
        long categoriaId = objectMapper.readTree(criada).path("id").asLong();

        mvc.perform(post("/api/v1/categorias")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"alimentação\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("categoria-nome-em-uso"));

        mvc.perform(put("/api/v1/categorias/{id}", categoriaPadraoId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Outro nome\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("categoria-nao-encontrada"));

        mvc.perform(put("/api/v1/categorias/{id}", categoriaId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Serviços digitais\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Serviços digitais"));

        mvc.perform(delete("/api/v1/categorias/{id}", categoriaId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        assertThat(categoriaRepository.findById(categoriaId)).isEmpty();
        assertThat(auditoriaRepository.findAll()).extracting("acao")
                .contains("CRIACAO_CATEGORIA", "ATUALIZACAO_CATEGORIA", "EXCLUSAO_CATEGORIA");
    }

    @Test
    void cadastraListaFiltraEditaEExcluiTransacoes() throws Exception {
        cadastrar("filipe@example.com");
        String token = login("filipe@example.com");
        long salarioId = idCategoria(token, "Salário");
        long personalizadaId = criarCategoria(token, "Mercado");
        LocalDate hoje = LocalDate.now();
        LocalDate ontem = hoje.minusDays(1);

        String entrada = criarTransacao(
                token, "ENTRADA", "3500.00", hoje, salarioId, "Salário mensal");
        long entradaId = objectMapper.readTree(entrada).path("id").asLong();
        criarTransacao(token, "SAIDA", "184.50", ontem, personalizadaId, "Supermercado");

        mvc.perform(get("/api/v1/transacoes")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(2))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.itens[0].descricao").value("Salário mensal"));

        mvc.perform(get("/api/v1/transacoes")
                        .header("Authorization", bearer(token))
                        .queryParam("tipo", "SAIDA")
                        .queryParam("categoriaId", String.valueOf(personalizadaId))
                        .queryParam("dataInicial", ontem.toString())
                        .queryParam("dataFinal", ontem.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].descricao").value("Supermercado"));

        mvc.perform(get("/api/v1/transacoes")
                        .header("Authorization", bearer(token))
                        .queryParam("dataInicial", hoje.toString())
                        .queryParam("dataFinal", ontem.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("periodo-invalido"));

        mvc.perform(put("/api/v1/transacoes/{id}", entradaId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transacaoJson(
                                "ENTRADA", "4200.50", hoje, salarioId, "Salário atualizado")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value(4200.50))
                .andExpect(jsonPath("$.descricao").value("Salário atualizado"));

        mvc.perform(delete("/api/v1/transacoes/{id}", entradaId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        assertThat(transacaoRepository.findById(entradaId)).isEmpty();
        assertThat(auditoriaRepository.findAll()).extracting("acao")
                .contains("EXCLUSAO_TRANSACAO");
    }

    @Test
    void validaDadosArquivaCategoriaUsadaEIsolaContextos() throws Exception {
        cadastrar("filipe@example.com");
        cadastrar("ana@example.com");
        String tokenFilipe = login("filipe@example.com");
        String tokenAna = login("ana@example.com");
        long categoriaFilipeId = criarCategoria(tokenFilipe, "Casa");

        mvc.perform(post("/api/v1/transacoes")
                        .header("Authorization", bearer(tokenAna))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transacaoJson(
                                "SAIDA", "10.00", LocalDate.now(), categoriaFilipeId, "Inválida")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("categoria-indisponivel"));

        mvc.perform(post("/api/v1/transacoes")
                        .header("Authorization", bearer(tokenFilipe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transacaoJson(
                                "SAIDA", "0", LocalDate.now().plusDays(1), categoriaFilipeId, " ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.valor").exists())
                .andExpect(jsonPath("$.campos.data").exists())
                .andExpect(jsonPath("$.campos.descricao").exists());

        criarTransacao(tokenFilipe, "SAIDA", "75.90", LocalDate.now(), categoriaFilipeId, "Internet");
        mvc.perform(delete("/api/v1/categorias/{id}", categoriaFilipeId)
                        .header("Authorization", bearer(tokenFilipe)))
                .andExpect(status().isNoContent());

        assertThat(categoriaRepository.findById(categoriaFilipeId).orElseThrow().getDataExclusao())
                .isNotNull();
        mvc.perform(get("/api/v1/categorias").header("Authorization", bearer(tokenFilipe)))
                .andExpect(jsonPath("$[?(@.nome == 'Casa')]").doesNotExist());
        mvc.perform(get("/api/v1/transacoes").header("Authorization", bearer(tokenFilipe)))
                .andExpect(jsonPath("$.itens[0].categoria.nome").value("Casa"));
        mvc.perform(post("/api/v1/transacoes")
                        .header("Authorization", bearer(tokenFilipe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transacaoJson(
                                "SAIDA", "20.00", LocalDate.now(), categoriaFilipeId, "Nova")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("categoria-indisponivel"));
    }

    @Test
    void paginaDeTransacoesTemVinteItens() throws Exception {
        cadastrar("filipe@example.com");
        String token = login("filipe@example.com");
        long categoriaId = idCategoria(token, "Lazer");
        for (int indice = 1; indice <= 21; indice++) {
            criarTransacao(
                    token, "SAIDA", String.valueOf(indice), LocalDate.now(),
                    categoriaId, "Transação " + indice);
        }

        mvc.perform(get("/api/v1/transacoes")
                        .header("Authorization", bearer(token))
                        .queryParam("pagina", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(20))
                .andExpect(jsonPath("$.totalItens").value(21))
                .andExpect(jsonPath("$.totalPaginas").value(2));
        mvc.perform(get("/api/v1/transacoes")
                        .header("Authorization", bearer(token))
                        .queryParam("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1));
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

    private String criarTransacao(String token, String tipo, String valor, LocalDate data,
                                  long categoriaId, String descricao) throws Exception {
        return mvc.perform(post("/api/v1/transacoes")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transacaoJson(tipo, valor, data, categoriaId, descricao)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String transacaoJson(String tipo, String valor, LocalDate data,
                                 long categoriaId, String descricao) throws Exception {
        LinkedHashMap<String, Object> dados = new LinkedHashMap<>();
        dados.put("tipo", tipo);
        dados.put("valor", new java.math.BigDecimal(valor));
        dados.put("data", data.toString());
        dados.put("categoriaId", categoriaId);
        dados.put("descricao", descricao);
        return objectMapper.writeValueAsString(dados);
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
