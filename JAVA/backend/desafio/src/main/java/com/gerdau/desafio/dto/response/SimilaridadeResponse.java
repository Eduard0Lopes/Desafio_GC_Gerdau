package com.gerdau.desafio.dto.response;

import com.gerdau.desafio.service.similaridade.StatusSimilaridade;

public record SimilaridadeResponse(
        String id,
        String descricao,
        double score,
        StatusSimilaridade status,
        String acaoRecomendada
) {
}
