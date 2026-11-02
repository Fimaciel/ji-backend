package br.edu.ifro.ji.controller;

import br.edu.ifro.ji.dto.AtualizacaoConfiguracaoRequest;
import br.edu.ifro.ji.dto.AtualizacaoPerfilRequest;
import br.edu.ifro.ji.dto.AtualizacaoTemaRequest;
import br.edu.ifro.ji.dto.ConfiguracaoUsuarioResponse;
import br.edu.ifro.ji.dto.ContaUsuarioResponse;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import br.edu.ifro.ji.service.ContaUsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios/me")
public class UsuarioController {

    private final ContaUsuarioService contaUsuarioService;

    public UsuarioController(ContaUsuarioService contaUsuarioService) {
        this.contaUsuarioService = contaUsuarioService;
    }

    @GetMapping
    ContaUsuarioResponse obter(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return contaUsuarioService.obter(principal.usuarioId());
    }

    @PutMapping("/perfil")
    ContaUsuarioResponse atualizarPerfil(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody AtualizacaoPerfilRequest request) {
        return contaUsuarioService.atualizarPerfil(principal.usuarioId(), request);
    }

    @PutMapping("/configuracao")
    ConfiguracaoUsuarioResponse atualizarConfiguracao(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody AtualizacaoConfiguracaoRequest request) {
        return contaUsuarioService.atualizarConfiguracao(principal.usuarioId(), request);
    }

    @PatchMapping("/configuracao/tema")
    ConfiguracaoUsuarioResponse atualizarTema(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody AtualizacaoTemaRequest request) {
        return contaUsuarioService.atualizarTema(principal.usuarioId(), request.tema());
    }

    @DeleteMapping("/configuracao")
    ConfiguracaoUsuarioResponse restaurarConfiguracao(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return contaUsuarioService.restaurarConfiguracao(principal.usuarioId());
    }

    @DeleteMapping
    ResponseEntity<Void> encerrarConta(@AuthenticationPrincipal UsuarioPrincipal principal) {
        contaUsuarioService.encerrarConta(principal.usuarioId());
        return ResponseEntity.noContent().build();
    }
}
