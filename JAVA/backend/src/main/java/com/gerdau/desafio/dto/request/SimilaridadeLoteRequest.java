package com.gerdau.desafio.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Criação em massa de códigos: a lista de itens informados pelo usuário
 * (uma linha por código) para avaliação de similaridade em lote.
 */
public record SimilaridadeLoteRequest(

        @NotEmpty(message = "Informe ao menos um código para avaliar.")
        @Size(max = 300, message = "Avalie no máximo 300 códigos por vez.")
        List<@Size(max = 200, message = "Cada código pode ter no máximo 200 caracteres.") String> textos,

        Integer limite,

        /** CS, GS, CF ou UM — vale para toda a lista. */
        String tipo

) {
}
