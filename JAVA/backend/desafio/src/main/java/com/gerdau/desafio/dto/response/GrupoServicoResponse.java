package com.gerdau.desafio.dto.response;

public record GrupoServicoResponse(
        String id,
        String nome,
        String nomeNormalizado,
        String codigoOriginal,
        String origem
) {
}