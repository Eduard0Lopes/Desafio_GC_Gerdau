package com.gerdau.desafio.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String recurso, String id) {
        super(String.format("%s não encontrado(a) com id: %s", recurso, id));
    }
}