package com.gerdau.desafio.dto.response;

/**
 * Resultado de uma linha da vinculação em massa.
 *
 * classificacao — o destino da linha (mesmo vocabulário da análise em lote):
 *   DISPONIVEL | código existente reutilizado
 *   CRIAR      | código novo criado
 *   BLOQUEADA  | não entrou (vazia, repetida na lista ou acima do limite SAP)
 *   FALHA      | a tentativa foi recusada pelo servidor
 *
 * decisao — a mesma régua do vínculo único: PERMITIR | ALERTAR | REVISAR
 * (null quando a linha não passou pela análise de similaridade).
 */
public record ItemVinculacaoResponse(

        int linha,
        String texto,
        String classificacao,
        String decisao,
        String motivo,
        String csId,
        String idCompleto,
        /** true = a requisição era repetida; nada novo foi criado */
        boolean requisicaoRepetida

) {
}
