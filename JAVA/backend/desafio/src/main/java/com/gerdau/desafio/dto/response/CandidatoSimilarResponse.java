package com.gerdau.desafio.dto.response;

import java.util.List;

/** Candidato semelhante encontrado pelo motor de similaridade. */
public record CandidatoSimilarResponse(
        int posicao,
        String id,
        String codigoOriginal,
        String descricao,
        double score,
        double scoreTokens,
        double scoreCaracteres,
        List<String> tokensEmComum,
        String motivo
) {
}
