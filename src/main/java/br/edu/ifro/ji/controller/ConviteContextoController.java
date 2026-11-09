package br.edu.ifro.ji.controller;

import br.edu.ifro.ji.dto.ConviteContextoResponse;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import br.edu.ifro.ji.service.ConviteContextoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/convites")
public class ConviteContextoController {

    private final ConviteContextoService service;

    public ConviteContextoController(ConviteContextoService service) {
        this.service = service;
    }

    @GetMapping
    List<ConviteContextoResponse> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return service.listar(principal);
    }

    @GetMapping("/token/{token}")
    ConviteContextoResponse detalharPorToken(@PathVariable String token) {
        return service.detalharPorToken(token);
    }

    @PostMapping("/{conviteId}/aceite")
    ConviteContextoResponse aceitar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long conviteId) {
        return service.aceitar(principal, conviteId);
    }

    @PostMapping("/{conviteId}/recusa")
    ConviteContextoResponse recusar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long conviteId) {
        return service.recusar(principal, conviteId);
    }
}
