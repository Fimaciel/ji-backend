package br.edu.ifro.ji.controller;

import br.edu.ifro.ji.dto.ImportacaoAnaliseResponse;
import br.edu.ifro.ji.dto.ImportacaoConfirmacaoRequest;
import br.edu.ifro.ji.dto.ImportacaoResponse;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import br.edu.ifro.ji.service.ImportacaoService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/importacoes")
public class ImportacaoController {

    private final ImportacaoService importacaoService;

    public ImportacaoController(ImportacaoService importacaoService) {
        this.importacaoService = importacaoService;
    }

    @PostMapping(value = "/analise", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ImportacaoAnaliseResponse analisar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestPart("arquivo") MultipartFile arquivo) {
        return importacaoService.analisar(principal, arquivo);
    }

    @PostMapping
    ResponseEntity<ImportacaoResponse> confirmar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody ImportacaoConfirmacaoRequest request,
            UriComponentsBuilder uriBuilder) {
        ImportacaoResponse importacao = importacaoService.confirmar(principal, request);
        return ResponseEntity.created(
                        uriBuilder.path("/api/v1/importacoes/{id}")
                                .buildAndExpand(importacao.id()).toUri())
                .body(importacao);
    }

    @GetMapping
    List<ImportacaoResponse> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return importacaoService.listar(principal);
    }

    @DeleteMapping("/ultima")
    ImportacaoResponse desfazerUltima(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return importacaoService.desfazerUltima(principal);
    }
}
