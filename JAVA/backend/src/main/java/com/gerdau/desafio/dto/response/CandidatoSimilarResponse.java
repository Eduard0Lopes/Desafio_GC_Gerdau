package com.gerdau.desafio.dto.response;

import java.util.List;

public record CandidatoSimilarResponse(
        int posicao,
        String id,
        String codigoOriginal,
        String descricao,
        double score,
        double scoreTokens,
        double scoreCaracteres,
        List<String> tokensEmComum,
        String motivo,
        boolean casoIsolado,
        String motivoCasoIsolado
) {
}
