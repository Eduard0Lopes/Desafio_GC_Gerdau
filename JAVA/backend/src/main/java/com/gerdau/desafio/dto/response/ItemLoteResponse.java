package com.gerdau.desafio.dto.response;

/**
 * Resultado da avaliação de uma linha da criação em massa.
 *
 * classificacao — o destino da linha:
 *   DISPONIVEL | já existe código para esta necessidade (reutilizar; não é criado)
 *   CRIAR      | precisa ser criado
 *   BLOQUEADA  | não pode entrar em massa (vazia, repetida na lista ou acima do limite SAP)
 *
 * situacao — o detalhe: JA_EXISTE | PARECIDO | OK | ALERTA | REPETIDA | LIMITE_SAP | VAZIA
 */
public record ItemLoteResponse(

        int linha,
        String texto,
        String classificacao,
        String situacao,
        String motivo,
        String decisao,
        double melhorScore,
        CandidatoSimilarResponse similar

) {
}
