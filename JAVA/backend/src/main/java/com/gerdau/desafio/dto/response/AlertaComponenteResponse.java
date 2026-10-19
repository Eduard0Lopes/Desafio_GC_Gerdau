package com.gerdau.desafio.dto.response;

public record AlertaComponenteResponse(
        String tipo,
        String descricaoInformada,
        String itemSimilarId,
        String itemSimilarNome,
        double score,
        String decisao,
        String motivo,
        boolean casoIsolado,
        String motivoCasoIsolado
) {
}
