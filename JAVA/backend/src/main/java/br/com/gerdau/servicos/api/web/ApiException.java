package br.com.gerdau.servicos.api.web;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** Erro de negócio com status HTTP, código estável (para o cliente decidir o fluxo) e dados extras. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;
    private final Map<String, ?> extras;

    public ApiException(HttpStatus status, String codigo, String message) {
        this(status, codigo, message, Map.of());
    }

    public ApiException(HttpStatus status, String codigo, String message, Map<String, ?> extras) {
        super(message);
        this.status = status;
        this.codigo = codigo;
        this.extras = extras;
    }

    public HttpStatus status() { return status; }
    public String codigo() { return codigo; }
    public Map<String, ?> extras() { return extras; }
}
