package br.edu.ifro.ji;

import br.edu.ifro.ji.model.Usuario;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;

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
@Import(ContaUsuarioApiIntegrationTest.MailTestConfig.class)
class ContaUsuarioApiIntegrationTest {

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
    private PasswordEncoder passwordEncoder;
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
    void limparBanco() {
        auditoriaRepository.deleteAllInBatch();
        tokenRepository.deleteAllInBatch();
        sessaoRepository.deleteAllInBatch();
        participanteRepository.deleteAllInBatch();
        contextoRepository.deleteAllInBatch();
        configuracaoRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
    }

    @Test
    void consultaEAtualizaPerfilSemPermitirCpfOuIdentificador() throws Exception {
        cadastrar("filipe@example.com", "52998224725");
        String token = login("filipe@example.com", "senha-segura", 200);

        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value("52998224725"))
                .andExpect(jsonPath("$.configuracao.moeda").value("BRL"))
                .andExpect(jsonPath("$.configuracao.tema").value("AUTOMATICO"));

        mvc.perform(put("/api/v1/usuarios/me/perfil")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfilJson("filipe.novo@example.com", "senha-incorreta", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("senha-atual-invalida"));

        mvc.perform(put("/api/v1/usuarios/me/perfil")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfilJson("  FILIPE.NOVO@example.com ",
                                "senha-segura", "nova-senha-segura")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Filipe atualizado"))
                .andExpect(jsonPath("$.email").value("filipe.novo@example.com"))
                .andExpect(jsonPath("$.cpf").value("52998224725"));

        Usuario usuario = usuarioRepository.findAll().getFirst();
        assertThat(usuario.getCpf()).isEqualTo("52998224725");
        assertThat(passwordEncoder.matches("nova-senha-segura", usuario.getSenha())).isTrue();
        assertThat(passwordEncoder.matches("senha-segura", usuario.getSenha())).isFalse();
        login("filipe.novo@example.com", "senha-segura", 401);
        login("filipe.novo@example.com", "nova-senha-segura", 200);
        assertThat(auditoriaRepository.findAll()).extracting("acao")
                .contains("ATUALIZACAO_PERFIL");
    }

    @Test
    void rejeitaEmailAtivoDuplicadoESenhaNovaDivergente() throws Exception {
        cadastrar("filipe@example.com", null);
        cadastrar("ana@example.com", null);
        String token = login("filipe@example.com", "senha-segura", 200);

        mvc.perform(put("/api/v1/usuarios/me/perfil")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfilJson("ana@example.com", "senha-segura", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("email-em-uso"));

        String divergente = perfilJson("filipe@example.com", "senha-segura", "nova-senha-segura")
                .replace("\"confirmacaoNovaSenha\":\"nova-senha-segura\"",
                        "\"confirmacaoNovaSenha\":\"outra-senha-segura\"");
        mvc.perform(put("/api/v1/usuarios/me/perfil")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(divergente))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("senhas-divergentes"));
    }

    @Test
    void configuraTemaERestauraPadroesComAuditoria() throws Exception {
        cadastrar("filipe@example.com", null);
        String token = login("filipe@example.com", "senha-segura", 200);

        mvc.perform(put("/api/v1/usuarios/me/configuracao")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"salarioMensal":5280.45,"moeda":"BRL","notificacoesAtivas":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salarioMensal").value(5280.45))
                .andExpect(jsonPath("$.notificacoesAtivas").value(false));

        mvc.perform(patch("/api/v1/usuarios/me/configuracao/tema")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tema\":\"ESCURO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tema").value("ESCURO"));

        mvc.perform(put("/api/v1/usuarios/me/configuracao")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"salarioMensal":-1,"moeda":"USD","notificacoesAtivas":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.salarioMensal").exists())
                .andExpect(jsonPath("$.campos.moeda").exists());

        mvc.perform(delete("/api/v1/usuarios/me/configuracao")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salarioMensal").value(0))
                .andExpect(jsonPath("$.moeda").value("BRL"))
                .andExpect(jsonPath("$.notificacoesAtivas").value(true))
                .andExpect(jsonPath("$.tema").value("AUTOMATICO"));

        assertThat(auditoriaRepository.findAll()).extracting("acao")
                .contains("ATUALIZACAO_CONFIGURACAO", "ALTERACAO_TEMA",
                        "RESTAURACAO_CONFIGURACAO");
    }

    @Test
    void encerramentoLogicoRevogaAcessosEPreservaRegistros() throws Exception {
        cadastrar("filipe@example.com", null);
        String token = login("filipe@example.com", "senha-segura", 200);
        mvc.perform(post("/api/v1/auth/recuperacao-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"filipe@example.com\"}"))
                .andExpect(status().isAccepted());

        long configuracoes = configuracaoRepository.count();
        long contextos = contextoRepository.count();
        long participacoes = participanteRepository.count();

        mvc.perform(delete("/api/v1/usuarios/me")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
        login("filipe@example.com", "senha-segura", 401);

        Usuario usuario = usuarioRepository.findAll().getFirst();
        assertThat(usuario.getDataExclusao()).isNotNull();
        assertThat(configuracaoRepository.count()).isEqualTo(configuracoes);
        assertThat(contextoRepository.count()).isEqualTo(contextos);
        assertThat(participanteRepository.count()).isEqualTo(participacoes);
        assertThat(sessaoRepository.findAll()).allSatisfy(sessao ->
                assertThat(sessao.getDataRevogacao()).isNotNull());
        assertThat(tokenRepository.findAll()).allSatisfy(tokenRedefinicao ->
                assertThat(tokenRedefinicao.isUtilizado()).isTrue());
        assertThat(auditoriaRepository.findAll()).extracting("acao")
                .contains("ENCERRAMENTO_CONTA");
    }

    private void cadastrar(String email, String cpf) throws Exception {
        LinkedHashMap<String, Object> dados = new LinkedHashMap<>();
        dados.put("nome", "Filipe");
        dados.put("sobrenome", "Lopes");
        if (cpf != null) {
            dados.put("cpf", cpf);
        }
        dados.put("email", email);
        dados.put("senha", "senha-segura");
        mvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dados)))
                .andExpect(status().isCreated());
    }

    private String login(String email, String senha, int statusEsperado) throws Exception {
        String corpo = objectMapper.writeValueAsString(
                new LinkedHashMap<>(java.util.Map.of("email", email, "senha", senha)));
        String resposta = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().is(statusEsperado))
                .andReturn().getResponse().getContentAsString();
        if (statusEsperado != 200) {
            return null;
        }
        JsonNode json = objectMapper.readTree(resposta);
        return json.path("tokenAcesso").asText();
    }

    private String perfilJson(String email, String senhaAtual, String novaSenha) throws Exception {
        LinkedHashMap<String, Object> dados = new LinkedHashMap<>();
        dados.put("nome", "Filipe atualizado");
        dados.put("sobrenome", "Lopes");
        dados.put("email", email);
        dados.put("dataNascimento", "1994-06-15");
        dados.put("senhaAtual", senhaAtual);
        if (novaSenha != null) {
            dados.put("novaSenha", novaSenha);
            dados.put("confirmacaoNovaSenha", novaSenha);
        }
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
