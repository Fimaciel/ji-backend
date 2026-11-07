package br.edu.ifro.ji.service;

import br.edu.ifro.ji.exception.RegraNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@Service
public class MesReferenciaService {

    private final Clock clock;

    public MesReferenciaService(Clock clock) {
        this.clock = clock;
    }

    public YearMonth interpretarOuAtual(String valor) {
        if (valor == null || valor.isBlank()) {
            return YearMonth.now(clock);
        }
        try {
            return YearMonth.parse(valor);
        } catch (DateTimeParseException exception) {
            throw new RegraNegocioException(
                    HttpStatus.BAD_REQUEST,
                    "mes-referencia-invalido",
                    "Informe o mês de referência no formato AAAA-MM."
            );
        }
    }

    public LocalDate primeiroDia(String valor) {
        return interpretarOuAtual(valor).atDay(1);
    }
}
