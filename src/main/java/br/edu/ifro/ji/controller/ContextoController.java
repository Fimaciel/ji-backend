package br.edu.ifro.ji.controller;

import br.edu.ifro.ji.dto.ContextoGerenciamentoResponse;
import br.edu.ifro.ji.dto.ContextoResponse;
import br.edu.ifro.ji.dto.CriarContextoRequest;
import br.edu.ifro.ji.dto.SelecionarContextoRequest;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import br.edu.ifro.ji.service.ContextoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contextos")
public class ContextoController {

    private final ContextoService service;

    public ContextoController(ContextoService service) {
        this.service = service;
    }

    @GetMapping
    List<ContextoGerenciamentoResponse> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return service.listar(principal);
    }

    @PostMapping
    ResponseEntity<ContextoGerenciamentoResponse> criar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CriarContextoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.criar(principal, request));
    }

    @PutMapping("/ativo")
    ContextoResponse selecionar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody SelecionarContextoRequest request) {
        return service.selecionar(principal, request.contextoId());
    }
}
