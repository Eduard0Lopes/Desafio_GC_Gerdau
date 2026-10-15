package com.gerdau.desafio.dto.response;

import java.util.List;
import java.util.Map;

/** Resultado da consulta de similaridade (seções 10.1 e 10.2). */
public record SimilaridadeResponse(

        String descricao,
        String descricaoNormalizada,
        int quantidadeCaracteres,
        boolean excedeLimiteSap,
        String status,
        String decisao,
        String acaoRecomendada,
        Map<String, Double> limiares,
        int totalItensAvaliados,
        List<CandidatoSimilarResponse> candidatos
) {
}
