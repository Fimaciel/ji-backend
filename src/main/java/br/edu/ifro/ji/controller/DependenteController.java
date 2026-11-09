package br.edu.ifro.ji.controller;

import br.edu.ifro.ji.dto.DependenteRequest;
import br.edu.ifro.ji.dto.DependenteResponse;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import br.edu.ifro.ji.service.DependenteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/dependentes")
public class DependenteController {

    private final DependenteService service;

    public DependenteController(DependenteService service) {
        this.service = service;
    }

    @GetMapping
    List<DependenteResponse> listar(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return service.listar(principal);
    }

    @PostMapping
    ResponseEntity<DependenteResponse> criar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody DependenteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(principal, request));
    }

    @PutMapping("/{dependenteId}")
    DependenteResponse atualizar(@AuthenticationPrincipal UsuarioPrincipal principal,
                                  @PathVariable Long dependenteId,
                                  @Valid @RequestBody DependenteRequest request) {
        return service.atualizar(principal, dependenteId, request);
    }

    @DeleteMapping("/{dependenteId}")
    ResponseEntity<Void> excluir(@AuthenticationPrincipal UsuarioPrincipal principal,
                                 @PathVariable Long dependenteId) {
        service.excluir(principal, dependenteId);
        return ResponseEntity.noContent().build();
    }
}
