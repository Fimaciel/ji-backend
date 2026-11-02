package br.edu.ifro.ji;

import br.edu.ifro.ji.repository.ConfiguracaoUsuarioRepository;
import br.edu.ifro.ji.repository.ContextoParticipanteRepository;
import br.edu.ifro.ji.repository.ContextoRepository;
import br.edu.ifro.ji.repository.LogAuditoriaRepository;
import br.edu.ifro.ji.repository.SessaoRepository;
import br.edu.ifro.ji.repository.TokenRedefinicaoSenhaRepository;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(AuthApiIntegrationTest.MailTestConfig.class)
class AuthApiIntegrationTest {

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
    private JwtDecoder jwtDecoder;
    @Autowired
    private CapturadorEmail capturadorEmail;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ConfiguracaoUsuarioRepository configuracaoRepository;
    @Autowired
    private ContextoRepository contextoRepository;
    @Autowired
    private ContextoParticipanteRepository participanteRepository;
    @Autowired
    private SessaoRepository sessaoRepository;
    @Autowired
    private TokenRedefinicaoSenhaRepository tokenRepository;
    @Autowired
    private LogAuditoriaRepository auditoriaRepository;

    @BeforeEach
    void limparCapturador() {
        capturadorEmail.limpar();
        auditoriaRepository.deleteAllInBatch();
        tokenRepository.deleteAllInBatch();
        sessaoRepository.deleteAllInBatch();
        participanteRepository.deleteAllInBatch();
        contextoRepository.deleteAllInBatch();
        configuracaoRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
    }

    @Test
    void cadastroCriaContaConfiguracaoContextoEParticipacaoNaMesmaOperacao() throws Exception {
        mvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroJson("  Filipe@Example.COM  ", "12345678901")))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("filipe@example.com"));

        assertThat(usuarioRepository.count()).isEqualTo(1);
        assertThat(configuracaoRepository.findAll()).singleElement().satisfies(configuracao -> {
            assertThat(configuracao.getMoeda()).isEqualTo("BRL");
            assertThat(configuracao.isNotificacoesAtivas()).isTrue();
            assertThat(configuracao.getTema().name()).isEqualTo("AUTOMATICO");
        });
        assertThat(contextoRepository.findAll()).singleElement().satisfies(contexto -> {
            assertThat(contexto.getNome()).isEqualTo("Pessoal");
            assertThat(contexto.getTipo().name()).isEqualTo("PESSOAL");
        });
        var usuario = usuarioRepository.findAll().getFirst();
        assertThat(participanteRepository
                .findByUsuarioIdAndDataSaidaIsNullOrderByDataIngressoAsc(usuario.getId()))
                .singleElement()
                .satisfies(participante ->
                        assertThat(participante.getPapel().name()).isEqualTo("ADMINISTRADOR"));
    }

    @Test
    void duplicidadesEValidacoesRetornamProblemJsonSemCriarEstruturasParciais() throws Exception {
        cadastrar("filipe@example.com", "12345678901");

        mvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroJson("FILIPE@example.com", null)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("email-em-uso"));
        mvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroJson("outro@example.com", "12345678901")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("cpf-em-uso"));
        mvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroJson("curta@example.com", null)
                                .replace("senha-segura", "curta")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("dados-invalidos"))
                .andExpect(jsonPath("$.campos.senha").exists());

        assertThat(usuarioRepository.count()).isEqualTo(1);
        assertThat(configuracaoRepository.count()).isEqualTo(1);
        assertThat(contextoRepository.count()).isEqualTo(1);
        assertThat(participanteRepository.count()).isEqualTo(1);
    }

    @Test
    void loginEmiteJwtSessaoValidaELogoutRevogaImediatamente() throws Exception {
        cadastrar("filipe@example.com", null);

        JsonNode login = login(" FILIPE@example.com ", "senha-segura", 200);
        String token = login.path("tokenAcesso").asText();
        var jwt = jwtDecoder.decode(token);
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("ji-back");
        assertThat(jwt.getAudience()).containsExactly("ji-front");
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getClaims()).doesNotContainKeys("email", "cpf", "senha");
        assertThat(login.path("selecaoContextoNecessaria").asBoolean()).isFalse();
        assertThat(login.path("contextoAtivoId").isNumber()).isTrue();

        mvc.perform(get("/api/v1/auth/sessao").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.email").value("filipe@example.com"))
                .andExpect(jsonPath("$.contextoAtivoId").isNumber());

        String adulterado = adulterarAssinatura(token);
        mvc.perform(get("/api/v1/auth/sessao").header("Authorization", bearer(adulterado)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("sessao-invalida"));

        mvc.perform(post("/api/v1/auth/logout").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/sessao").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());

        assertThat(sessaoRepository.findAll()).singleElement().satisfies(sessao ->
                assertThat(sessao.getDataRevogacao()).isNotNull());
        assertThat(auditoriaRepository.findAll())
                .extracting("acao")
                .containsExactlyInAnyOrder("LOGIN", "LOGOUT");
    }

    @Test
    void credenciaisInvalidasEOuAusenciaDeJwtRetornam401() throws Exception {
        cadastrar("filipe@example.com", null);

        login("filipe@example.com", "senha-errada", 401);
        mvc.perform(get("/api/v1/auth/sessao"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("sessao-invalida"));
    }

    @Test
    void recuperacaoTemRespostaGenericaTokenUnicoETrocaRevogaSessoes() throws Exception {
        cadastrar("filipe@example.com", null);
        String tokenSessaoAntiga = login("filipe@example.com", "senha-segura", 200)
                .path("tokenAcesso").asText();

        String corpoInexistente = mvc.perform(post("/api/v1/auth/recuperacao-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ninguem@example.com\"}"))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        String corpoExistente = mvc.perform(post("/api/v1/auth/recuperacao-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"filipe@example.com\"}"))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        assertThat(corpoExistente).isEqualTo(corpoInexistente);
        assertThat(capturadorEmail.mensagens).hasSize(1);
        String primeiroToken = capturadorEmail.ultimoToken();

        mvc.perform(post("/api/v1/auth/recuperacao-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"filipe@example.com\"}"))
                .andExpect(status().isAccepted());
        assertThat(capturadorEmail.mensagens).hasSize(2);
        assertThat(tokenRepository.findAll()).hasSize(2).allSatisfy(token -> {
            assertThat(token.getTokenHash()).hasSize(64).doesNotContain("token=");
            assertThat(token.getDataExpiracao()).isEqualTo(token.getDataCriacao().plusSeconds(1800));
        }).extracting("utilizado").containsExactlyInAnyOrder(true, false);

        String tentativaTokenAnterior = redefinicaoJson(primeiroToken, "nova-senha-segura");
        mvc.perform(post("/api/v1/auth/redefinicao-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tentativaTokenAnterior))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("token-redefinicao-invalido"));

        String tokenRedefinicao = capturadorEmail.ultimoToken();
        mvc.perform(get("/api/v1/auth/redefinicao-senha/validacao")
                        .queryParam("token", tokenRedefinicao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("valido"))
                .andExpect(jsonPath("$.email").value("filipe@example.com"))
                .andExpect(jsonPath("$.expiraEm").exists());
        String redefinicao = redefinicaoJson(tokenRedefinicao, "nova-senha-segura");
        mvc.perform(post("/api/v1/auth/redefinicao-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(redefinicao))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/redefinicao-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(redefinicao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("token-redefinicao-invalido"));
        mvc.perform(get("/api/v1/auth/redefinicao-senha/validacao")
                        .queryParam("token", tokenRedefinicao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("utilizado"));
        mvc.perform(get("/api/v1/auth/sessao")
                        .header("Authorization", bearer(tokenSessaoAntiga)))
                .andExpect(status().isUnauthorized());
        login("filipe@example.com", "senha-segura", 401);
        login("filipe@example.com", "nova-senha-segura", 200);

        assertThat(auditoriaRepository.findAll()).extracting("acao")
                .contains("SOLICITACAO_REDEFINICAO_SENHA", "REDEFINICAO_SENHA");
    }

    @Test
    void jsonMalformadoUsaCodigoDeErroEstavel() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("corpo-invalido"));
    }

    private void cadastrar(String email, String cpf) throws Exception {
        mvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroJson(email, cpf)))
                .andExpect(status().isCreated());
    }

    private JsonNode login(String email, String senha, int statusEsperado) throws Exception {
        var dados = new java.util.LinkedHashMap<String, String>();
        dados.put("email", email);
        dados.put("senha", senha);
        String corpo = objectMapper.writeValueAsString(dados);
        var resultado = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().is(statusEsperado))
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }

    private String cadastroJson(String email, String cpf) throws Exception {
        var dados = new java.util.LinkedHashMap<String, Object>();
        dados.put("nome", "Filipe");
        dados.put("sobrenome", "Lopes");
        if (cpf != null) {
            dados.put("cpf", cpf);
        }
        dados.put("email", email);
        dados.put("senha", "senha-segura");
        return objectMapper.writeValueAsString(dados);
    }

    private String redefinicaoJson(String token, String senha) throws Exception {
        var dados = new java.util.LinkedHashMap<String, String>();
        dados.put("token", token);
        dados.put("novaSenha", senha);
        dados.put("confirmacaoSenha", senha);
        return objectMapper.writeValueAsString(dados);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String adulterarAssinatura(String token) {
        int indice = token.lastIndexOf('.') + 1;
        char original = token.charAt(indice);
        char alterado = original == 'A' ? 'B' : 'A';
        return token.substring(0, indice) + alterado + token.substring(indice + 1);
    }

    @TestConfiguration
    static class MailTestConfig {

        @Bean
        @Primary
        CapturadorEmail capturadorEmail() {
            return new CapturadorEmail();
        }
    }

    static class CapturadorEmail implements PasswordResetMailSender {

        private final List<Mensagem> mensagens = new ArrayList<>();

        @Override
        public void enviar(String destinatario, String nome, String linkRedefinicao) {
            mensagens.add(new Mensagem(destinatario, nome, linkRedefinicao));
        }

        void limpar() {
            mensagens.clear();
        }

        String ultimoToken() {
            String link = mensagens.getLast().link();
            return link.substring(link.indexOf("token=") + "token=".length());
        }

        private record Mensagem(String destinatario, String nome, String link) {
        }
    }
}
