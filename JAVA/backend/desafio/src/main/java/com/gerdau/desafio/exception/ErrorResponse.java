package com.gerdau.desafio.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato de erro consistente da API (especificação, seção 7):
 * code, message, details (campos), correlationId.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        Map<String, String> campos,
        String correlationId
) {
    public ErrorResponse(int status, String error, String code, String message, String path, String correlationId) {
        this(LocalDateTime.now(), status, error, code, message, path, null, correlationId);
    }

    public ErrorResponse(int status, String error, String code, String message, String path,
                         Map<String, String> campos, String correlationId) {
        this(LocalDateTime.now(), status, error, code, message, path, campos, correlationId);
    }
}
