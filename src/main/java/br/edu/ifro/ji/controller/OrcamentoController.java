package br.edu.ifro.ji.controller;

import br.edu.ifro.ji.dto.OrcamentoRequest;
import br.edu.ifro.ji.dto.OrcamentoResponse;
import br.edu.ifro.ji.dto.ResumoOrcamentosResponse;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import br.edu.ifro.ji.service.OrcamentoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/orcamentos")
public class OrcamentoController {

    private final OrcamentoService orcamentoService;

    public OrcamentoController(OrcamentoService orcamentoService) {
        this.orcamentoService = orcamentoService;
    }

    @GetMapping
    ResumoOrcamentosResponse listar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(required = false) String mes) {
        return orcamentoService.listar(principal, mes);
    }

    @PostMapping
    ResponseEntity<OrcamentoResponse> criar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody OrcamentoRequest request,
            UriComponentsBuilder uriBuilder) {
        OrcamentoResponse orcamento = orcamentoService.criar(principal, request);
        return ResponseEntity.created(
                        uriBuilder.path("/api/v1/orcamentos/{id}")
                                .buildAndExpand(orcamento.id()).toUri())
                .body(orcamento);
    }

    @PutMapping("/{orcamentoId}")
    OrcamentoResponse editar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long orcamentoId,
            @Valid @RequestBody OrcamentoRequest request) {
        return orcamentoService.editar(principal, orcamentoId, request);
    }

    @DeleteMapping("/{orcamentoId}")
    ResponseEntity<Void> excluir(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long orcamentoId) {
        orcamentoService.excluir(principal, orcamentoId);
        return ResponseEntity.noContent().build();
    }
}
