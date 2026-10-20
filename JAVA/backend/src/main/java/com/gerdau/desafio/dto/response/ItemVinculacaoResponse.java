package com.gerdau.desafio.dto.response;

/**
 * Resultado de uma linha da vinculação em massa.
 *
 * classificacao — DISPONIVEL (código existente reutilizado) | CRIAR (código novo)
 *               | BLOQUEADA (não pode entrar) | FALHA (a tentativa foi recusada)
 */
public record ItemVinculacaoResponse(

        int linha,
        String texto,
        String classificacao,
        String situacao,
        String motivo,
        String csId,
        String idCompleto,
        String idRegistro,
        String status,
        /** true = a requisição era repetida; nada novo foi criado */
        boolean requisicaoRepetida

) {
}
