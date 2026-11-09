package br.edu.ifro.ji;

import br.edu.ifro.ji.repository.ConfiguracaoUsuarioRepository;
import br.edu.ifro.ji.repository.ContextoParticipanteRepository;
import br.edu.ifro.ji.repository.ContextoRepository;
import br.edu.ifro.ji.repository.ConviteContextoRepository;
import br.edu.ifro.ji.repository.DependenteRepository;
import br.edu.ifro.ji.repository.LogAuditoriaRepository;
import br.edu.ifro.ji.repository.SessaoRepository;
import br.edu.ifro.ji.repository.TokenRedefinicaoSenhaRepository;
import br.edu.ifro.ji.repository.UsuarioRepository;
import br.edu.ifro.ji.service.ConviteMailSender;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(ColaboracaoApiIntegrationTest.MailTestConfig.class)
class ColaboracaoApiIntegrationTest {

    private static final String SEGREDO_JWT = Base64.getEncoder().encodeToString(
            "segredo-jwt-de-testes-com-32-bytes".getBytes(StandardCharsets.UTF_8));

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registry) {
        registry.add("ji.security.jwt.secret-base64", () -> SEGREDO_JWT);
        registry.add("ji.mail.mode", () -> "test");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CapturadorConvites capturador;
    @Autowired LogAuditoriaRepository auditoriaRepository;
    @Autowired ConviteContextoRepository conviteRepository;
    @Autowired DependenteRepository dependenteRepository;
    @Autowired TokenRedefinicaoSenhaRepository tokenRepository;
    @Autowired SessaoRepository sessaoRepository;
    @Autowired ContextoParticipanteRepository participanteRepository;
    @Autowired ContextoRepository contextoRepository;
    @Autowired ConfiguracaoUsuarioRepository configuracaoRepository;
    @Autowired UsuarioRepository usuarioRepository;

    @BeforeEach
    void limparBanco() {
        capturador.limpar();
        auditoriaRepository.deleteAllInBatch();
        conviteRepository.deleteAllInBatch();
        dependenteRepository.deleteAllInBatch();
        tokenRepository.deleteAllInBatch();
        sessaoRepository.deleteAllInBatch();
        participanteRepository.deleteAllInBatch();
        contextoRepository.deleteAllInBatch();
        configuracaoRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
    }

    @Test
    void enviaReenviaCancelaERecriaConviteDeColaborador() throws Exception {
        cadastrar("admin@example.com");
        cadastrar("convidado@example.com");
        String admin = login("admin@example.com");
        String convidado = login("convidado@example.com");
        long contextoId = criarESelecionarContexto(admin, "Família");

        long conviteId = convidar(admin, "convidado@example.com");
        assertThat(capturador.links).hasSize(1);
        assertThat(capturador.links.getFirst()).contains("/convite/");

        mvc.perform(post("/api/v1/contextos/ativo/convites")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"convidado@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("convite-pendente-existente"));

        mvc.perform(post("/api/v1/contextos/ativo/convites/{id}/reenvio", conviteId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"));
        assertThat(capturador.links).hasSize(2);

        mvc.perform(delete("/api/v1/contextos/ativo/convites/{id}", conviteId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        long novoConviteId = convidar(admin, "convidado@example.com");
        mvc.perform(post("/api/v1/convites/{id}/aceite", novoConviteId)
                        .header("Authorization", bearer(convidado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACEITO"));

        assertThat(participanteRepository.existsByContextoIdAndUsuarioIdAndDataSaidaIsNull(
                contextoId, usuario("convidado@example.com"))).isTrue();
        assertThat(auditoriaRepository.findAll()).extracting("acao")
                .contains("ENVIO_CONVITE", "REENVIO_CONVITE", "CANCELAMENTO_CONVITE", "ACEITE_CONVITE");
    }

    @Test
    void cadastraDependentesEVinculaAcessoSomenteLeituraNoAceite() throws Exception {
        cadastrar("admin@example.com");
        cadastrar("leitura@example.com");
        String admin = login("admin@example.com");
        String leitura = login("leitura@example.com");

        mvc.perform(post("/api/v1/dependentes")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Fora do compartilhado",
                                "dataNascimento", "2018-05-10",
                                "tipo", "PASSIVO"
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("administrador-obrigatorio"));

        long contextoId = criarESelecionarContexto(admin, "Casa");

        mvc.perform(post("/api/v1/dependentes")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Criança Teste",
                                "dataNascimento", "2018-05-10",
                                "tipo", "PASSIVO"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("PASSIVO"))
                .andExpect(jsonPath("$.participanteId").isEmpty());

        String resposta = mvc.perform(post("/api/v1/dependentes")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Pessoa Leitura",
                                "dataNascimento", "2004-02-20",
                                "tipo", "LEITURA",
                                "email", "leitura@example.com"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("LEITURA"))
                .andReturn().getResponse().getContentAsString();
        long dependenteId = objectMapper.readTree(resposta).path("id").asLong();
        long conviteId = conviteRepository.findByContextoIdOrderByDataCriacaoDesc(contextoId)
                .getFirst().getId();

        mvc.perform(post("/api/v1/convites/{id}/aceite", conviteId)
                        .header("Authorization", bearer(leitura)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papelProposto").value("DEPENDENTE"));

        mvc.perform(get("/api/v1/dependentes").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].usuarioVinculado").value("leitura@example.com"));

        mvc.perform(put("/api/v1/dependentes/{id}", dependenteId)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Pessoa Leitura Atualizada",
                                "dataNascimento", "2004-02-20",
                                "tipo", "LEITURA"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Pessoa Leitura Atualizada"));

        selecionarContexto(leitura, contextoId);
        mvc.perform(post("/api/v1/categorias")
                        .header("Authorization", bearer(leitura))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Não permitida\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("contexto-somente-leitura"));

        mvc.perform(delete("/api/v1/dependentes/{id}", dependenteId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        assertThat(dependenteRepository.findById(dependenteId).orElseThrow().getDataExclusao()).isNotNull();
        assertThat(participanteRepository.existsByContextoIdAndUsuarioIdAndDataSaidaIsNull(
                contextoId, usuario("leitura@example.com"))).isFalse();
    }

    @Test
    void alteraPapeisPreservaHistoricoEProtegeUltimoAdministrador() throws Exception {
        cadastrar("admin@example.com");
        cadastrar("colega@example.com");
        String admin = login("admin@example.com");
        String colega = login("colega@example.com");
        long contextoId = criarESelecionarContexto(admin, "República");
        long conviteId = convidar(admin, "colega@example.com");
        mvc.perform(post("/api/v1/convites/{id}/aceite", conviteId)
                        .header("Authorization", bearer(colega)))
                .andExpect(status().isOk());
        selecionarContexto(colega, contextoId);

        long participacaoColega = participanteRepository
                .findByContextoIdAndUsuarioIdAndDataSaidaIsNull(contextoId, usuario("colega@example.com"))
                .orElseThrow().getId();
        long participacaoAdmin = participanteRepository
                .findByContextoIdAndUsuarioIdAndDataSaidaIsNull(contextoId, usuario("admin@example.com"))
                .orElseThrow().getId();

        mvc.perform(patch("/api/v1/contextos/ativo/participantes/{id}/papel", participacaoColega)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"papel\":\"ADMINISTRADOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("ADMINISTRADOR"));

        mvc.perform(patch("/api/v1/contextos/ativo/participantes/{id}/papel", participacaoAdmin)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"papel\":\"COLABORADOR\"}"))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/v1/contextos/ativo/participantes/me")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        mvc.perform(delete("/api/v1/contextos/ativo/participantes/me")
                        .header("Authorization", bearer(colega)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ultimo-administrador"));

        mvc.perform(get("/api/v1/contextos/ativo/participantes")
                        .header("Authorization", bearer(colega)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ativo").value(false))
                .andExpect(jsonPath("$[0].dataSaida").isNotEmpty())
                .andExpect(jsonPath("$[1].papel").value("ADMINISTRADOR"));
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

    private long criarESelecionarContexto(String token, String nome) throws Exception {
        String resposta = mvc.perform(post("/api/v1/contextos")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", nome))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long contextoId = objectMapper.readTree(resposta).path("id").asLong();
        selecionarContexto(token, contextoId);
        return contextoId;
    }

    private void selecionarContexto(String token, long contextoId) throws Exception {
        mvc.perform(put("/api/v1/contextos/ativo")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("contextoId", contextoId))))
                .andExpect(status().isOk());
    }

    private long convidar(String token, String email) throws Exception {
        String resposta = mvc.perform(post("/api/v1/contextos/ativo/convites")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.papelProposto").value("COLABORADOR"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resposta).path("id").asLong();
    }

    private long usuario(String email) {
        return usuarioRepository.findByEmailIgnoreCaseAndDataExclusaoIsNull(email).orElseThrow().getId();
    }

    private String bearer(String token) { return "Bearer " + token; }

    static class CapturadorConvites implements ConviteMailSender {
        private final List<String> links = new ArrayList<>();
        @Override
        public void enviar(String destinatario, String contexto, String remetente, String linkConvite) {
            links.add(linkConvite);
        }
        void limpar() { links.clear(); }
    }

    @TestConfiguration
    static class MailTestConfig {
        @Bean @Primary PasswordResetMailSender passwordResetMailSender() {
            return (destinatario, nome, linkRedefinicao) -> { };
        }
        @Bean @Primary CapturadorConvites conviteMailSender() {
            return new CapturadorConvites();
        }
    }
}
