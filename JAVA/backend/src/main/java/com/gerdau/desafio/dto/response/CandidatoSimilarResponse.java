package com.gerdau.desafio.dto.response;

import java.util.List;

public record CandidatoSimilarResponse(

        int posicao,
        String id,
        String codigoOriginal,
        String descricao,
        double score,
        double scoreTokens,
        double scoreCaracteres,
        List<String> tokensEmComum,
        String motivo,
        boolean casoIsolado,
        String motivoCasoIsolado,
        /** componente ao qual o match pertence: CS, GS, CF ou UM */
        String tipo

) {

    /** Compatibilidade: sem tipo informado, o candidato é de Código de Serviço (CS). */
    public CandidatoSimilarResponse(
            int posicao, String id, String codigoOriginal, String descricao,
            double score, double scoreTokens, double scoreCaracteres, List<String> tokensEmComum,
            String motivo, boolean casoIsolado, String motivoCasoIsolado) {
        this(posicao, id, codigoOriginal, descricao, score, scoreTokens, scoreCaracteres,
                tokensEmComum, motivo, casoIsolado, motivoCasoIsolado, "CS");
    }
}
