package br.edu.ifro.ji.controller;

import br.edu.ifro.ji.dto.PaginaResponse;
import br.edu.ifro.ji.dto.TransacaoRequest;
import br.edu.ifro.ji.dto.TransacaoResponse;
import br.edu.ifro.ji.model.TipoTransacao;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import br.edu.ifro.ji.service.TransacaoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/v1/transacoes")
public class TransacaoController {

    private final TransacaoService transacaoService;

    public TransacaoController(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    @GetMapping
    PaginaResponse<TransacaoResponse> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(required = false) TipoTransacao tipo,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String busca) {
        return transacaoService.listar(
                principal, pagina, dataInicial, dataFinal, tipo, categoriaId, busca);
    }

    @PostMapping
    ResponseEntity<TransacaoResponse> criar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody TransacaoRequest request,
            UriComponentsBuilder uriBuilder) {
        TransacaoResponse transacao = transacaoService.criar(principal, request);
        return ResponseEntity.created(
                        uriBuilder.path("/api/v1/transacoes/{id}")
                                .buildAndExpand(transacao.id()).toUri())
                .body(transacao);
    }

    @PutMapping("/{transacaoId}")
    TransacaoResponse editar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long transacaoId,
            @Valid @RequestBody TransacaoRequest request) {
        return transacaoService.editar(principal, transacaoId, request);
    }

    @DeleteMapping("/{transacaoId}")
    ResponseEntity<Void> excluir(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long transacaoId) {
        transacaoService.excluir(principal, transacaoId);
        return ResponseEntity.noContent().build();
    }
}
