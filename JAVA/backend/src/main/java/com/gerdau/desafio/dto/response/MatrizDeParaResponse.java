package com.gerdau.desafio.dto.response;

import java.time.Instant;

public record MatrizDeParaResponse(
        String id,
        String newId,
        String descricaoCanonica,
        String grupoServico,
        String unidadeMedida,
        String decision,
        Double score,
        String reason,
        LegacyDataResponse legacyData,
        String statusRevisao,
        String reviewedBy,
        Instant reviewedAt,
        String observacaoRevisao
) {

    public record LegacyDataResponse(
            String legacyId,
            String legacyDescription,
            String legacySource,
            Long codigoFornecedor,
            Boolean alertaSap,
            Integer quantidadeCaracteres
    ) {
    }
}
