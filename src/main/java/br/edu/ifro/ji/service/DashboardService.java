package br.edu.ifro.ji.service;

import br.edu.ifro.ji.dto.CategoriaDashboardResponse;
import br.edu.ifro.ji.dto.DashboardResponse;
import br.edu.ifro.ji.dto.FluxoDiarioResponse;
import br.edu.ifro.ji.dto.TransacaoResponse;
import br.edu.ifro.ji.repository.TransacaoRepository;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;

@Service
public class DashboardService {

    private final TransacaoRepository transacaoRepository;
    private final ContextoAtivoService contextoAtivoService;
    private final MesReferenciaService mesReferenciaService;
    private final Clock clock;

    public DashboardService(TransacaoRepository transacaoRepository,
                            ContextoAtivoService contextoAtivoService,
                            MesReferenciaService mesReferenciaService,
                            Clock clock) {
        this.transacaoRepository = transacaoRepository;
        this.contextoAtivoService = contextoAtivoService;
        this.mesReferenciaService = mesReferenciaService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse obter(UsuarioPrincipal principal, String mesInformado) {
        ContextoAtivoService.AcessoContexto acesso =
                contextoAtivoService.obterParaLeitura(principal);
        YearMonth mes = mesReferenciaService.interpretarOuAtual(mesInformado);
        var inicio = mes.atDay(1);
        var fim = mes.atEndOfMonth();
        Long contextoId = acesso.contexto().getId();

        BigDecimal entradas = transacaoRepository.somarEntradas(contextoId, inicio, fim);
        BigDecimal saidas = transacaoRepository.somarSaidas(contextoId, inicio, fim);
        BigDecimal acumulado = BigDecimal.ZERO;
        var fluxo = new ArrayList<FluxoDiarioResponse>();
        for (TransacaoRepository.FluxoDiarioProjection dia
                : transacaoRepository.resumirFluxoDiario(contextoId, inicio, fim)) {
            acumulado = acumulado.add(dia.getEntradas()).subtract(dia.getSaidas());
            fluxo.add(new FluxoDiarioResponse(
                    dia.getData(), dia.getEntradas(), dia.getSaidas(), acumulado));
        }

        var categorias = transacaoRepository
                .resumirSaidasPorCategoria(contextoId, inicio, fim)
                .stream()
                .limit(5)
                .map(categoria -> new CategoriaDashboardResponse(
                        categoria.getCategoriaId(),
                        categoria.getCategoriaNome(),
                        categoria.getTotal(),
                        saidas.signum() == 0
                                ? BigDecimal.ZERO
                                : categoria.getTotal()
                                        .multiply(BigDecimal.valueOf(100))
                                        .divide(saidas, 2, RoundingMode.HALF_UP)
                ))
                .toList();

        var recentes = transacaoRepository
                .findTop5ByContextoIdAndDataBetweenOrderByDataDescIdDesc(
                        contextoId, inicio, fim)
                .stream()
                .map(TransacaoResponse::de)
                .toList();

        return new DashboardResponse(
                mes.toString(),
                entradas,
                saidas,
                entradas.subtract(saidas),
                fluxo,
                categorias,
                recentes,
                Instant.now(clock)
        );
    }
}
