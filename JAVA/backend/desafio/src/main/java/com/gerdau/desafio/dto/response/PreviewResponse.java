package com.gerdau.desafio.dto.response;

import java.util.List;

/**
 * Prévia antes de salvar (seção 10.2): composição, descrição final, contagem de
 * caracteres, existência de código igual, similares e decisão recomendada.
 * A regra anti-duplicidade cobre todos os componentes ({@code alertasComponentes}).
 */
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

        String status,
        String decisao,
        String acaoRecomendada,

        double melhorScore,

        List<CandidatoSimilarResponse> similares,

        List<AlertaComponenteResponse> alertasComponentes
) {
}
