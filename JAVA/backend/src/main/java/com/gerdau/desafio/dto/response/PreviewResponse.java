package com.gerdau.desafio.dto.response;

import java.util.List;

public record PreviewResponse(

        String idCompleto,

        ComponentePreviewResponse grupoServico,
        ComponentePreviewResponse codigoFornecedor,
        ComponentePreviewResponse codigoServico,
        ComponentePreviewResponse unidadeMedida,

        String descricaoSnapshot,

        int quantidadeCaracteres,

        boolean excedeLimiteSap,

        boolean idCompletoJaExiste,

        String statusExistente,

        String status,
        String decisao,
        String acaoRecomendada,

        double melhorScore,

        List<CandidatoSimilarResponse> similares,

        List<AlertaComponenteResponse> alertasComponentes
) {
}
