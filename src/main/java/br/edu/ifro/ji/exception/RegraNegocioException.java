package br.edu.ifro.ji.exception;

import org.springframework.http.HttpStatus;

public class RegraNegocioException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public RegraNegocioException(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}
