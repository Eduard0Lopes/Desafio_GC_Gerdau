package com.gerdau.desafio.dto.response;

public record CodigoFornecedorResponse(
        String id,
        Long codigoFornecedorSAP,
        String nome,
        String nomeNormalizado,
        String origem
) {
}
