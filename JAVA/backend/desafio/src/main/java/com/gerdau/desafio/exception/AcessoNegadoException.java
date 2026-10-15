package com.gerdau.desafio.exception;

/** Acesso negado por perfil insuficiente (seção 12.2) — HTTP 403. */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String message) {
        super(message);
    }
}
