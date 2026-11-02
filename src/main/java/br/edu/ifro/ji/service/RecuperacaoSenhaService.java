package br.edu.ifro.ji.service;

import br.edu.ifro.ji.dto.RedefinicaoSenhaRequest;
import br.edu.ifro.ji.dto.ValidacaoTokenResponse;
import br.edu.ifro.ji.exception.RegraNegocioException;
import br.edu.ifro.ji.model.TokenRedefinicaoSenha;
import br.edu.ifro.ji.model.Usuario;
import br.edu.ifro.ji.repository.SessaoRepository;
import br.edu.ifro.ji.repository.TokenRedefinicaoSenhaRepository;
import br.edu.ifro.ji.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class RecuperacaoSenhaService {

    private final UsuarioRepository usuarioRepository;
    private final TokenRedefinicaoSenhaRepository tokenRepository;
    private final SessaoRepository sessaoRepository;
    private final TokenSeguroService tokenSeguroService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailSender mailSender;
    private final AuditoriaService auditoriaService;
    private final Clock clock;
    private final String frontendUrl;

    public RecuperacaoSenhaService(
            UsuarioRepository usuarioRepository,
            TokenRedefinicaoSenhaRepository tokenRepository,
            SessaoRepository sessaoRepository,
            TokenSeguroService tokenSeguroService,
            PasswordEncoder passwordEncoder,
            PasswordResetMailSender mailSender,
            AuditoriaService auditoriaService,
            Clock clock,
            @Value("${ji.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.sessaoRepository = sessaoRepository;
        this.tokenSeguroService = tokenSeguroService;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public void solicitar(String emailInformado) {
        String email = emailInformado.trim().toLowerCase(Locale.ROOT);
        usuarioRepository.findByEmailIgnoreCaseAndDataExclusaoIsNull(email)
                .ifPresent(this::criarEEnviarToken);
    }

    @Transactional
    public void redefinir(RedefinicaoSenhaRequest request) {
        if (!request.novaSenha().equals(request.confirmacaoSenha())) {
            throw new RegraNegocioException(
                    HttpStatus.BAD_REQUEST,
                    "senhas-divergentes",
                    "A nova senha e a confirmação devem ser iguais."
            );
        }

        Instant agora = Instant.now(clock);
        TokenRedefinicaoSenha token = tokenRepository
                .findByTokenHash(tokenSeguroService.hash(request.token()))
                .filter(encontrado -> encontrado.estaValidoEm(agora))
                .orElseThrow(this::tokenInvalido);

        Usuario usuario = token.getUsuario();
        usuario.redefinirSenha(passwordEncoder.encode(request.novaSenha()), agora);
        token.marcarComoUtilizado();
        sessaoRepository.revogarTodasDoUsuario(usuario.getId(), agora);
        auditoriaService.registrar(
                usuario,
                null,
                "REDEFINICAO_SENHA",
                "USUARIO",
                usuario.getId(),
                "Senha redefinida e sessões anteriores revogadas."
        );
    }

    @Transactional(readOnly = true)
    public ValidacaoTokenResponse validar(String tokenAberto) {
        Instant agora = Instant.now(clock);
        return tokenRepository.findByTokenHash(tokenSeguroService.hash(tokenAberto))
                .map(token -> criarValidacao(token, agora))
                .orElseGet(() -> new ValidacaoTokenResponse("invalido", null, null));
    }

    private ValidacaoTokenResponse criarValidacao(TokenRedefinicaoSenha token, Instant agora) {
        if (!token.getUsuario().estaAtivo()) {
            return new ValidacaoTokenResponse("invalido", null, null);
        }
        if (token.isUtilizado()) {
            return new ValidacaoTokenResponse("utilizado", null, null);
        }
        if (!token.getDataExpiracao().isAfter(agora)) {
            return new ValidacaoTokenResponse("expirado", null, token.getDataExpiracao());
        }
        return new ValidacaoTokenResponse(
                "valido",
                token.getUsuario().getEmail(),
                token.getDataExpiracao()
        );
    }

    private void criarEEnviarToken(Usuario usuario) {
        Instant agora = Instant.now(clock);
        tokenRepository.invalidarTokensPendentes(usuario.getId());

        String tokenAberto = tokenSeguroService.gerar();
        TokenRedefinicaoSenha token = tokenRepository.save(new TokenRedefinicaoSenha(
                usuario,
                tokenSeguroService.hash(tokenAberto),
                agora,
                agora.plus(Duration.ofMinutes(30))
        ));
        auditoriaService.registrar(
                usuario,
                null,
                "SOLICITACAO_REDEFINICAO_SENHA",
                "TOKEN_REDEFINICAO_SENHA",
                token.getId(),
                "Link de redefinição solicitado."
        );

        String link = frontendUrl + "/redefinir-senha?token=" +
                URLEncoder.encode(tokenAberto, StandardCharsets.UTF_8);
        mailSender.enviar(usuario.getEmail(), usuario.getNome(), link);
    }

    private RegraNegocioException tokenInvalido() {
        return new RegraNegocioException(
                HttpStatus.BAD_REQUEST,
                "token-redefinicao-invalido",
                "O link de redefinição é inválido, já foi utilizado ou expirou."
        );
    }
}
