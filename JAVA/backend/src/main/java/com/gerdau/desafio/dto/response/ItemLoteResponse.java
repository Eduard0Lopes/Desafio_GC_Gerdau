package com.gerdau.desafio.dto.response;

public record ItemLoteResponse(

        int linha,
        String texto,
        String classificacao,
        String situacao,
        String motivo,
        String decisao,
        double melhorScore,
        CandidatoSimilarResponse similar

) {
}
