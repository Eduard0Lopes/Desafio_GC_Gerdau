package com.gerdau.desafio.dto.response;

import java.util.List;

public record CodigoCompletoResponse(
        String id,
        String idCompleto,
        String gsId,
        String cfId,
        String csId,
        String umId,
        String descricaoSnapshot,
        String origem,
        List<String> legacyIds,
        String status,
        String decisaoSimilaridade,
        Double melhorScore,
        String justificativa,
        String usuarioCriacaoId,
        String createdAt,
        String aprovadoPor,
        String observacaoAprovacao
) {
}
