package br.edu.ifro.ji.controller;

import br.edu.ifro.ji.dto.CategoriaRequest;
import br.edu.ifro.ji.dto.CategoriaResponse;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import br.edu.ifro.ji.service.CategoriaService;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    List<CategoriaResponse> listar(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return categoriaService.listar(principal);
    }

    @PostMapping
    ResponseEntity<CategoriaResponse> criar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CategoriaRequest request,
            UriComponentsBuilder uriBuilder) {
        CategoriaResponse categoria = categoriaService.criar(principal, request);
        return ResponseEntity.created(
                        uriBuilder.path("/api/v1/categorias/{id}")
                                .buildAndExpand(categoria.id()).toUri())
                .body(categoria);
    }

    @PutMapping("/{categoriaId}")
    CategoriaResponse editar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long categoriaId,
            @Valid @RequestBody CategoriaRequest request) {
        return categoriaService.editar(principal, categoriaId, request);
    }

    @DeleteMapping("/{categoriaId}")
    ResponseEntity<Void> excluir(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long categoriaId) {
        categoriaService.excluir(principal, categoriaId);
        return ResponseEntity.noContent().build();
    }
}
