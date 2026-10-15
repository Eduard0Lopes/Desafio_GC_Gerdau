package com.gerdau.desafio.dto.response;

import java.util.List;

/**
 * Item da fila de governança — tanto do histórico (matriz DUPLICIDADE/ALERTA,
 * pré-carregada com os scores reais) quanto de novos cadastros enviados à validação.
 */
public record ItemGovernancaResponse(

        String origem,           // HISTORICO | NOVO_CADASTRO
        String id,               // id do documento
        String identificador,    // newId (CS-xxxxxx) ou idCompleto
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
        List<CandidatoSimilarResponse> similares
) {
}
