package br.edu.ifro.ji.controller;

import br.edu.ifro.ji.dto.AlterarPapelRequest;
import br.edu.ifro.ji.dto.ConvidarContextoRequest;
import br.edu.ifro.ji.dto.ConviteEnviadoResponse;
import br.edu.ifro.ji.dto.ParticipanteContextoResponse;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import br.edu.ifro.ji.service.GestaoConviteService;
import br.edu.ifro.ji.service.ParticipanteContextoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contextos/ativo")
public class ColaboracaoContextoController {

    private final GestaoConviteService conviteService;
    private final ParticipanteContextoService participanteService;

    public ColaboracaoContextoController(GestaoConviteService conviteService,
                                         ParticipanteContextoService participanteService) {
        this.conviteService = conviteService;
        this.participanteService = participanteService;
    }

    @GetMapping("/convites")
    List<ConviteEnviadoResponse> listarConvites(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return conviteService.listarEnviados(principal);
    }

    @PostMapping("/convites")
    ResponseEntity<ConviteEnviadoResponse> convidar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody ConvidarContextoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conviteService.convidarColaborador(principal, request));
    }

    @PostMapping("/convites/{conviteId}/reenvio")
    ConviteEnviadoResponse reenviar(@AuthenticationPrincipal UsuarioPrincipal principal,
                                     @PathVariable Long conviteId) {
        return conviteService.reenviar(principal, conviteId);
    }

    @DeleteMapping("/convites/{conviteId}")
    ResponseEntity<Void> cancelar(@AuthenticationPrincipal UsuarioPrincipal principal,
                                  @PathVariable Long conviteId) {
        conviteService.cancelar(principal, conviteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/participantes")
    List<ParticipanteContextoResponse> listarParticipantes(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return participanteService.listar(principal);
    }

    @PatchMapping("/participantes/{participanteId}/papel")
    ParticipanteContextoResponse alterarPapel(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long participanteId,
            @Valid @RequestBody AlterarPapelRequest request) {
        return participanteService.alterarPapel(principal, participanteId, request.papel());
    }

    @DeleteMapping("/participantes/{participanteId}")
    ResponseEntity<Void> removerParticipante(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long participanteId) {
        participanteService.remover(principal, participanteId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/participantes/me")
    ResponseEntity<Void> sair(@AuthenticationPrincipal UsuarioPrincipal principal) {
        participanteService.sair(principal);
        return ResponseEntity.noContent().build();
    }
}
