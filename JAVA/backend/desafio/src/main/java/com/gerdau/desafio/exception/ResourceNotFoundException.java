package com.gerdau.desafio.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String recurso, Object id) {
        super(String.format("%s não encontrado(a) com id: %s", recurso, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
