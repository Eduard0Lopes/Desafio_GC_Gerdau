package com.gerdau.desafio.dto.response;

import java.util.List;

public record ItemGovernancaResponse(

        String origem,           
        String id,               
        String identificador,    
        String descricao,
        String grupoServico,
        String fornecedor,
        String unidadeMedida,
        String legacyId,
        String legacyDescription,
        String decision,
        double score,
        String reason,
        String solicitante,
        String justificativa,
        String status,
        String natureza,
        List<CandidatoSimilarResponse> similares,
        boolean casoIsolado,
        String motivoCasoIsolado
) {
}
