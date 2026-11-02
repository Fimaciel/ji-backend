package br.edu.ifro.ji.controller;

import br.edu.ifro.ji.dto.AutenticacaoResponse;
import br.edu.ifro.ji.dto.CadastroRequest;
import br.edu.ifro.ji.dto.LoginRequest;
import br.edu.ifro.ji.dto.MensagemResponse;
import br.edu.ifro.ji.dto.RecuperacaoSenhaRequest;
import br.edu.ifro.ji.dto.RedefinicaoSenhaRequest;
import br.edu.ifro.ji.dto.SessaoResponse;
import br.edu.ifro.ji.dto.UsuarioResponse;
import br.edu.ifro.ji.dto.ValidacaoTokenResponse;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import br.edu.ifro.ji.service.AutenticacaoService;
import br.edu.ifro.ji.service.RecuperacaoSenhaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String MENSAGEM_RECUPERACAO =
            "Se existir uma conta ativa para o e-mail informado, o link será enviado.";

    private final AutenticacaoService autenticacaoService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;

    public AuthController(AutenticacaoService autenticacaoService,
                          RecuperacaoSenhaService recuperacaoSenhaService) {
        this.autenticacaoService = autenticacaoService;
        this.recuperacaoSenhaService = recuperacaoSenhaService;
    }

    @PostMapping("/cadastro")
    ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody CadastroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(autenticacaoService.cadastrar(request));
    }

    @PostMapping("/login")
    AutenticacaoResponse login(@Valid @RequestBody LoginRequest request) {
        return autenticacaoService.login(request);
    }

    @PostMapping("/recuperacao-senha")
    ResponseEntity<MensagemResponse> solicitarRecuperacao(
            @Valid @RequestBody RecuperacaoSenhaRequest request) {
        recuperacaoSenhaService.solicitar(request.email());
        return ResponseEntity.accepted()
                .body(new MensagemResponse(MENSAGEM_RECUPERACAO));
    }

    @PostMapping("/redefinicao-senha")
    ResponseEntity<Void> redefinirSenha(@Valid @RequestBody RedefinicaoSenhaRequest request) {
        recuperacaoSenhaService.redefinir(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/redefinicao-senha/validacao")
    ValidacaoTokenResponse validarTokenRedefinicao(@RequestParam String token) {
        return recuperacaoSenhaService.validar(token);
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@AuthenticationPrincipal UsuarioPrincipal principal) {
        autenticacaoService.logout(principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessao")
    SessaoResponse obterSessao(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return autenticacaoService.obterSessao(principal);
    }
}
