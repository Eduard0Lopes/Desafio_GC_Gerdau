package com.gerdau.desafio.dto.response;

public record CodigoFornecedorResponse(
        String id,
        String codigoOriginal,
        String nomeNormalizado,
        String origem
) {
}