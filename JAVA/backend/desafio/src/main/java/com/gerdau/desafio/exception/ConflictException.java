package com.gerdau.desafio.exception;

/** Conflito de regra de negócio (ex.: criação bloqueada pela política de similaridade) — HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
