package com.gerdau.desafio.dto.response;

import java.time.Instant;

/**
 * Documento MESCLADO (base saneada × matriz) — o "banco" do Time de IA,
 * com os campos de governança da seção 9.6 (reviewedBy/reviewedAt).
 */
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
