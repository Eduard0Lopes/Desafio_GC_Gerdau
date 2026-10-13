package com.gerdau.desafio.dto.response;

public record CodigoServicoResponse(
        String id,
        String codigoOriginal,
        String descricao,
        String descricaoNormalizada,
        int quantidadeCaracteres,
        String origem
) {
}