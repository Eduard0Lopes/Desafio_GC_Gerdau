package com.gerdau.desafio.dto.response;

/** Componente resolvido na prévia: existente ou que seria criado. */
public record ComponentePreviewResponse(
        String id,
        String nome,
        boolean criadoNovo
) {
}
