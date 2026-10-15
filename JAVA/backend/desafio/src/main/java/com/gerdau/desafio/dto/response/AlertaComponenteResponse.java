package com.gerdau.desafio.dto.response;

/** Alerta de duplicidade em um componente específico (GS, CF, CS ou UM). */
public record AlertaComponenteResponse(
        String tipo,
        String descricaoInformada,
        String itemSimilarId,
        String itemSimilarNome,
        double score,
        String decisao,
        String motivo
) {
}
