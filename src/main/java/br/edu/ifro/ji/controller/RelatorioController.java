package br.edu.ifro.ji.controller;

import br.edu.ifro.ji.dto.RelatorioResumoResponse;
import br.edu.ifro.ji.model.FormatoRelatorio;
import br.edu.ifro.ji.model.TipoTransacao;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import br.edu.ifro.ji.service.RelatorioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/resumo")
    RelatorioResumoResponse resumir(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataInicial,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataFinal,
            @RequestParam(required = false) TipoTransacao tipo,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) List<Long> contextoIds) {
        return relatorioService.resumir(
                principal, dataInicial, dataFinal, tipo, categoriaId, contextoIds);
    }

    @GetMapping("/exportacao")
    ResponseEntity<byte[]> exportar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataInicial,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataFinal,
            @RequestParam(required = false) TipoTransacao tipo,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) List<Long> contextoIds,
            @RequestParam FormatoRelatorio formato) {
        RelatorioService.ArquivoRelatorio arquivo = relatorioService.exportar(
                principal, dataInicial, dataFinal, tipo, categoriaId, contextoIds, formato);
        return ResponseEntity.ok()
                .contentType(arquivo.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(arquivo.nomeArquivo(), StandardCharsets.UTF_8)
                                .build().toString())
                .body(arquivo.conteudo());
    }
}
