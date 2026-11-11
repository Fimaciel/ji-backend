package br.edu.ifro.ji.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RegraNegocioException.class)
    ProblemDetail tratarRegraNegocio(RegraNegocioException exception,
                                     HttpServletRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                exception.getStatus(), exception.getMessage());
        problema.setTitle("Não foi possível concluir a operação");
        problema.setType(URI.create("https://ji.local/erros/" + exception.getCodigo()));
        problema.setInstance(URI.create(request.getRequestURI()));
        problema.setProperty("codigo", exception.getCodigo());
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail tratarValidacao(MethodArgumentNotValidException exception,
                                  HttpServletRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Revise os campos informados.");
        problema.setTitle("Dados inválidos");
        problema.setType(URI.create("https://ji.local/erros/dados-invalidos"));
        problema.setInstance(URI.create(request.getRequestURI()));

        Map<String, String> campos = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(erro ->
                campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        problema.setProperty("campos", campos);
        problema.setProperty("codigo", "dados-invalidos");
        return problema;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail tratarCorpoInvalido(HttpMessageNotReadableException exception,
                                      HttpServletRequest request) {
        return criarProblema(
                HttpStatus.BAD_REQUEST,
                "corpo-invalido",
                "Corpo da requisição inválido",
                "Verifique o formato do JSON e os tipos dos campos informados.",
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail tratarConflitoDeIntegridade(DataIntegrityViolationException exception,
                                               HttpServletRequest request) {
        return criarProblema(
                HttpStatus.CONFLICT,
                "dados-em-conflito",
                "Não foi possível concluir a operação",
                "Os dados informados entram em conflito com um registro existente.",
                request
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail tratarArquivoMuitoGrande(MaxUploadSizeExceededException exception,
                                            HttpServletRequest request) {
        return criarProblema(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "arquivo-muito-grande",
                "Arquivo muito grande",
                "O arquivo deve ter no máximo 10 MB.",
                request
        );
    }

    private ProblemDetail criarProblema(HttpStatus status, String codigo, String titulo,
                                        String detalhe, HttpServletRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setType(URI.create("https://ji.local/erros/" + codigo));
        problema.setInstance(URI.create(request.getRequestURI()));
        problema.setProperty("codigo", codigo);
        return problema;
    }
}
