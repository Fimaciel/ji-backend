package br.edu.ifro.ji;

import br.edu.ifro.ji.model.ConviteContexto;
import br.edu.ifro.ji.model.PapelParticipante;
import br.edu.ifro.ji.repository.ConfiguracaoUsuarioRepository;
import br.edu.ifro.ji.repository.ContextoParticipanteRepository;
import br.edu.ifro.ji.repository.ContextoRepository;
import br.edu.ifro.ji.repository.ConviteContextoRepository;
import br.edu.ifro.ji.repository.LogAuditoriaRepository;
import br.edu.ifro.ji.repository.SessaoRepository;
import br.edu.ifro.ji.repository.TokenRedefinicaoSenhaRepository;
import br.edu.ifro.ji.repository.UsuarioRepository;
import br.edu.ifro.ji.service.PasswordResetMailSender;
import br.edu.ifro.ji.service.TokenSeguroService;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(ContextosConvitesApiIntegrationTest.MailTestConfig.class)
class ContextosConvitesApiIntegrationTest {

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
    private ConviteContextoRepository conviteRepository;
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
    @Autowired
    private TokenSeguroService tokenSeguroService;

    @BeforeEach
    void limparBanco() {
        auditoriaRepository.deleteAllInBatch();
        conviteRepository.deleteAllInBatch();
        tokenRepository.deleteAllInBatch();
        sessaoRepository.deleteAllInBatch();
        participanteRepository.deleteAllInBatch();
        contextoRepository.deleteAllInBatch();
        configuracaoRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
    }

    @Test
    void criaListaESelecionaContextoPersistindoNaSessao() throws Exception {
        cadastrar("filipe@example.com");
        String token = login("filipe@example.com").path("tokenAcesso").asText();

        String criado = mvc.perform(post("/api/v1/contextos")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "  Casa Lopes  "))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Casa Lopes"))
                .andExpect(jsonPath("$.tipo").value("COMPARTILHADO"))
                .andExpect(jsonPath("$.papel").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.quantidadeParticipantes").value(1))
                .andReturn().getResponse().getContentAsString();
        long contextoId = objectMapper.readTree(criado).path("id").asLong();

        mvc.perform(post("/api/v1/contextos")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").exists());

        mvc.perform(get("/api/v1/contextos")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tipo").value("PESSOAL"))
                .andExpect(jsonPath("$[0].ativo").value(true))
                .andExpect(jsonPath("$[1].ativo").value(false));

        mvc.perform(put("/api/v1/contextos/ativo")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("contextoId", contextoId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(contextoId));

        mvc.perform(get("/api/v1/auth/sessao")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contextoAtivoId").value(contextoId))
                .andExpect(jsonPath("$.selecaoContextoNecessaria").value(false))
                .andExpect(jsonPath("$.contextos.length()").value(2));

        JsonNode novoLogin = login("filipe@example.com");
        assertThat(novoLogin.path("selecaoContextoNecessaria").asBoolean()).isTrue();
        assertThat(novoLogin.path("contextoAtivoId").isNull()).isTrue();
        String tokenSemContexto = novoLogin.path("tokenAcesso").asText();

        mvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", bearer(tokenSemContexto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("contexto-ativo-ausente"));

        mvc.perform(put("/api/v1/contextos/ativo")
                        .header("Authorization", bearer(tokenSemContexto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("contextoId", contextoId))))
                .andExpect(status().isOk());

        assertThat(auditoriaRepository.findAll()).extracting("acao")
                .contains("CRIACAO_CONTEXTO", "SELECAO_CONTEXTO");
    }

    @Test
    void listaAceitaERecusaConvitesEExpiraOsVencidos() throws Exception {
        cadastrar("remetente@example.com");
        cadastrar("convidado@example.com");
        cadastrar("outra@example.com");
        String tokenRemetente = login("remetente@example.com").path("tokenAcesso").asText();
        String tokenConvidado = login("convidado@example.com").path("tokenAcesso").asText();
        String tokenOutra = login("outra@example.com").path("tokenAcesso").asText();
        long contextoAceite = criarContexto(tokenRemetente, "Casa compartilhada");
        long contextoRecusa = criarContexto(tokenRemetente, "Viagem");
        long contextoExpirado = criarContexto(tokenRemetente, "República");
        var remetente = usuarioRepository
                .findByEmailIgnoreCaseAndDataExclusaoIsNull("remetente@example.com")
                .orElseThrow();
        Instant agora = Instant.now();
        String tokenPublico = "convite-publico-seguro";

        ConviteContexto conviteAceite = conviteRepository.save(new ConviteContexto(
                contextoRepository.findById(contextoAceite).orElseThrow(),
                remetente,
                "convidado@example.com",
                PapelParticipante.COLABORADOR,
                tokenSeguroService.hash(tokenPublico),
                agora,
                agora.plus(7, ChronoUnit.DAYS)
        ));
        ConviteContexto conviteRecusa = conviteRepository.save(new ConviteContexto(
                contextoRepository.findById(contextoRecusa).orElseThrow(),
                remetente,
                "convidado@example.com",
                PapelParticipante.COLABORADOR,
                tokenSeguroService.hash("token-recusa"),
                agora,
                agora.plus(7, ChronoUnit.DAYS)
        ));
        conviteRepository.save(new ConviteContexto(
                contextoRepository.findById(contextoExpirado).orElseThrow(),
                remetente,
                "convidado@example.com",
                PapelParticipante.COLABORADOR,
                tokenSeguroService.hash("token-expirado"),
                agora.minus(8, ChronoUnit.DAYS),
                agora.minus(1, ChronoUnit.DAYS)
        ));

        mvc.perform(get("/api/v1/convites/token/{token}", tokenPublico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contextoNome").value("Casa compartilhada"))
                .andExpect(jsonPath("$.status").value("PENDENTE"));

        mvc.perform(get("/api/v1/convites")
                        .header("Authorization", bearer(tokenConvidado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[2].status").value("EXPIRADO"));

        mvc.perform(post("/api/v1/convites/{id}/aceite", conviteAceite.getId())
                        .header("Authorization", bearer(tokenOutra)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("convite-invalido"));

        mvc.perform(post("/api/v1/convites/{id}/aceite", conviteAceite.getId())
                        .header("Authorization", bearer(tokenConvidado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACEITO"));
        assertThat(participanteRepository
                .existsByContextoIdAndUsuarioIdAndDataSaidaIsNull(
                        contextoAceite,
                        usuarioRepository
                                .findByEmailIgnoreCaseAndDataExclusaoIsNull("convidado@example.com")
                                .orElseThrow().getId()))
                .isTrue();

        mvc.perform(post("/api/v1/convites/{id}/recusa", conviteRecusa.getId())
                        .header("Authorization", bearer(tokenConvidado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECUSADO"));

        mvc.perform(post("/api/v1/convites/{id}/aceite", conviteAceite.getId())
                        .header("Authorization", bearer(tokenConvidado)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("convite-indisponivel"));

        assertThat(auditoriaRepository.findAll()).extracting("acao")
                .contains("ACEITE_CONVITE", "RECUSA_CONVITE");
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

    private JsonNode login(String email) throws Exception {
        String resposta = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "senha", "senha-segura"
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resposta);
    }

    private long criarContexto(String token, String nome) throws Exception {
        String resposta = mvc.perform(post("/api/v1/contextos")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", nome))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resposta).path("id").asLong();
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
